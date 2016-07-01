package modelsMethods

import javax.inject.Inject

import database.MyPostgresDriver.api._
import database.{EventArtistRelation, MyDBTableDefinitions, MyPostgresDriver}
import logger.LoggerHelper
import models.Artist
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future
import scala.language.postfixOps

class ArtistMethods @Inject()(protected val dbConfigProvider: DatabaseConfigProvider, genreMethods: GenreMethods)
    extends HasDatabaseConfigProvider[MyPostgresDriver]
    with MyDBTableDefinitions
    with LoggerHelper {

  def save(artist: Artist): Future[Artist] = {
    db.run((for {
      artistFound <- artists.filter(_.facebookUrl === artist.facebookUrl).result.headOption
      result <- artistFound
        .map(DBIO.successful)
        .getOrElse(artists returning artists.map(_.facebookId) += artist)
    } yield result match {
      case artist: Artist => artist
      case id: String => artist.copy(facebookId = id)
    }).transactionally)
  }

  def saveWithEventRelation(artist: Artist, eventId: String): Future[Artist] = save(artist) flatMap { savedArtist =>
    saveEventRelation(EventArtistRelation(eventId, savedArtist.facebookId)) map(_ => savedArtist)
  }

  def saveEventRelation(eventArtistRelation: EventArtistRelation): Future[Int] = 
    db.run(eventsArtists += eventArtistRelation)
}
