package modelsMethods

import javax.inject.Inject

import database.MyPostgresDriver.api._
import database._
import logger.LoggerHelper
import models.Genre
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future
import scala.language.postfixOps

class GenreMethods @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
    extends HasDatabaseConfigProvider[MyPostgresDriver] with MyDBTableDefinitions with LoggerHelper {

  def save(genre: Genre): Future[Genre] = db.run((for {
    genreFound <- genres.filter(_.name === genre.name).result.headOption
    result <- genreFound.map(DBIO.successful).getOrElse(genres returning genres.map(_.id) +=
      genre.copy(name = genre.name.toLowerCase))
  } yield result match {
    case g: Genre => g
    case id: Int => genre.copy(id = Option(id))
  }).transactionally)

  def saveWithArtistRelation(genre: Genre, artistId: String): Future[Option[ArtistGenreRelation]] = {
    save(genre) flatMap {
      _.id match {
        case None =>
          log("Genre.saveWithArtistRelation: genre saved returned None as id")
          Future.successful(None)

        case Some(id) =>
          saveArtistRelation(ArtistGenreRelation(artistId, id)) map { Option(_) }
      }
    }
  }

  def saveArtistRelation(artistGenreRelation: ArtistGenreRelation): Future[ArtistGenreRelation] = {
    db.run((for {
      artistGenreFound <- artistsGenres
        .filter(relation => relation.artistFacebookId === artistGenreRelation.artistId &&
          relation.genreId === artistGenreRelation.genreId)
        .result
        .headOption
      result <- artistGenreFound
        .map(DBIO.successful)
        .getOrElse(artistsGenres returning artistsGenres.map(_.artistFacebookId) += artistGenreRelation)
    } yield result match {
      case artistGenre: ArtistGenreRelation =>
        val updatedArtistGenreRelation = artistGenre.copy(weight = artistGenre.weight + 1)
        updateArtistRelation(updatedArtistGenreRelation) map {
          case int if int != 1 =>
            log("Genre.saveArtistRelation: not exactly one row was updated")
            artistGenre

          case _ =>
            updatedArtistGenreRelation
        }

      case id: String =>
        Future(artistGenreRelation.copy(artistId = id))
    }).transactionally) flatMap(artistGenreRelation => artistGenreRelation)
  }

  def updateArtistRelation(artistGenreRelation: ArtistGenreRelation): Future[Int] =
    db.run(artistsGenres.filter(relation => relation.artistFacebookId === artistGenreRelation.artistId &&
      relation.genreId === artistGenreRelation.genreId).update(artistGenreRelation))

  def genresStringToGenresSet(genres: String): Set[Genre] = {
    val refactoredGenres = genres
      .toLowerCase
      .filter(_ >= ' ')
      .replaceAll("'", "")
      .replaceAll("&", "")
      .replaceAll("musique", "")
      .replaceAll("musik", "")
      .replaceAll("music", "")
    val genresSplitBySpecialCharacters = refactoredGenres
      .split(",")
      .flatMap(_.split("/"))
      .flatMap(_.split(":"))
      .flatMap(_.split(";"))
      .map(_.trim)
      .filter(_.nonEmpty)

    genresSplitBySpecialCharacters.length match {
      case twoOrPlusElement if twoOrPlusElement > 1 =>
        genresSplitBySpecialCharacters.map { genreName => Genre(None, genreName) }.toSet
      case _ =>
        """([%/+\.;]|& )""".r
          .split(refactoredGenres)
          .filterNot(_ == "") match {
          case list if list.length != 1 =>
            list.map { genreName => new Genre(None, genreName) }.toSet
          case listOfOneItem =>
            listOfOneItem(0)
              .split("\\s+")
              .filter(_.nonEmpty)
              .map { genreName => new Genre(None, genreName.stripSuffix(".")) }
              .toSet
      }
    }
  }
}