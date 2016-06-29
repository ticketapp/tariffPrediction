package genresDomain

import logger.LoggerHelper

import scala.language.postfixOps

case class Genre (id: Option[Int] = None, name: String, icon: Char = 'a') {
  require(name.nonEmpty, "It is forbidden to create a genre without a name.")
}

case class GenreWithWeight(genre: Genre, weight: Int = 1)

trait GenreMethods extends LoggerHelper {

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