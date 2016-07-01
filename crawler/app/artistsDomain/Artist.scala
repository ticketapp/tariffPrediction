package artistsDomain

import javax.inject.Inject

import APIs.FacebookAPI.{Cover, FacebookArtist}
import APIs.{FacebookAPI, FormatResponses}
import logger.LoggerHelper
import models.{Artist, ArtistWithWeightedGenres}
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.functional.syntax._
import play.api.libs.json._
import websites.Websites
import websites.Websites.normalizeUrl

import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.matching.Regex

final case class PatternAndArtist(searchPattern: String, artistWithWeightedGenres: ArtistWithWeightedGenres)

@SerialVersionUID(42L)
final case class EventIdArtists(eventId: String, artistsWithWeightedGenres: Seq[ArtistWithWeightedGenres])

class ArtistMethods @Inject()(websites: Websites,
                              facebookAPI: FacebookAPI)
    extends LoggerHelper
    with FormatResponses {

  def normalizeArtistName(artistName: String): String = artistName
    .toLowerCase
    .replaceAll("officiel", "")
    .replaceAll("fanpage", "")
    .replaceAll("official", "")
    .replaceAll("fb", "")
    .replaceAll("facebook", "")
    .replaceAll("page", "")
    .trim
    .replaceAll("""\s+""", " ")

  def getEventuallyArtistsInEventTitle(eventName: String,
                                       websites: Set[String]): Future[Seq[ArtistWithWeightedGenres]] = {
    val artistNames = splitArtistNamesInTitle(eventName)

    Future.sequence(artistNames.map(artistName => getArtistsForAnEvent(artistName, websites))).map(_.flatten)
  }

  def getArtistsForAnEvent(artistName: String, eventWebSites: Set[String]): Future[Seq[ArtistWithWeightedGenres]] = {
    val websitesFound = eventWebSites collect {
      case website if website.contains("facebook") && website.contains(artistName.toLowerCase) => website
    }

    websitesFound match {
      case nonEmptyWebsites if nonEmptyWebsites.nonEmpty =>
        val artists = nonEmptyWebsites map { website =>
          normalizeFacebookUrl(website) match {
            case Some(url) =>
              getFacebookArtistByFacebookUrl(url) flatMap {
                case Some(artist) => Future.successful(Option(artist))
                case _ => getFacebookArtistByFacebookUrl(url)
              }

            case _ =>
              Future.successful(None)
          }
        }
        Future.sequence(artists).map(_.flatten.toVector)

      case _ =>
        getFacebookArtist(artistName, eventWebSites)
    }
  }

  def getFacebookArtist(artistName: String, eventWebSites: Set[String]): Future[Seq[ArtistWithWeightedGenres]] = {
    facebookAPI.getArtists(artistName).flatMap(response => readFacebookArtists(response)).flatMap {
      case noArtist if noArtist.isEmpty && artistName.split("\\W+").length >= 2 =>
        val nestedEventuallyArtists = artistName.split("\\W+").toSeq.map { name =>
          getArtistsForAnEvent(name.trim, eventWebSites)
        }
        Future.sequence(nestedEventuallyArtists) map(_.toVector.flatten)

      case foundArtists =>
        Future.successful(filterFacebookArtistsForEvent(foundArtists, artistName, eventWebSites))
    }
  }

  def filterFacebookArtistsForEvent(artists: Seq[ArtistWithWeightedGenres],
                                    artistName: String,
                                    eventWebsites: Set[String]): Seq[ArtistWithWeightedGenres] = artists match {
    case onlyOneArtist: Seq[ArtistWithWeightedGenres] if onlyOneArtist.size == 1 &&
      onlyOneArtist.head.artist.name.toLowerCase == artistName.toLowerCase =>
        onlyOneArtist

    case otherCase: Seq[ArtistWithWeightedGenres] =>
      otherCase.flatMap { artist: ArtistWithWeightedGenres =>
        if ((artist.artist.websites intersect eventWebsites).nonEmpty) Option(artist)
        else None
      }

    case _  =>
      artists
  }

  def readMaybeFacebookUrl(soundCloudWebProfilesResponse: JsValue): Option[String] = {
    val facebookUrlReads = (
      (__ \ "url").read[String] and
        (__ \ "service").read[String]
      )((url: String, service: String) => (url, service))

    val collectOnlyFacebookUrls = Reads.seq(facebookUrlReads).map { urlService =>
      urlService.collect { case (url: String, "facebook") => normalizeUrl(url) }
    }

    soundCloudWebProfilesResponse.asOpt[scala.Seq[String]](collectOnlyFacebookUrls) match {
      case Some(facebookUrls: Seq[String]) if facebookUrls.nonEmpty => Option(facebookUrls.head)
      case _ => None
    }
  }

  def normalizeFacebookUrl(facebookUrl: String): Option[String] = {
    val firstNormalization = facebookUrl.toLowerCase match {
      case urlWithProfile: String if urlWithProfile contains "profile.php?id=" =>
        Option(urlWithProfile.substring(urlWithProfile.lastIndexOf("=") + 1))

      case alreadyNormalizedUrl: String =>
        if (alreadyNormalizedUrl.indexOf("facebook.com/") > -1) {
          val normalizedUrl = alreadyNormalizedUrl.substring(alreadyNormalizedUrl.indexOf("facebook.com/") + 13)
          if (normalizedUrl.indexOf("pages/") > -1) {
            val idRegex = new Regex("/[0-9]+")
            idRegex.findAllIn(normalizedUrl).toSeq.headOption match {
              case Some(id) => Option(id.replace("/", ""))
              case None => None
            }
          } else if (normalizedUrl.indexOf("/") > -1) Option(normalizedUrl.take(normalizedUrl.indexOf("/")))
            else Option(normalizedUrl)
        } else
          Option(alreadyNormalizedUrl)

      case _ =>
        None
    }

    firstNormalization match {
      case Some(urlWithArguments) if urlWithArguments contains "?" =>
        Option(urlWithArguments.slice(0, urlWithArguments.lastIndexOf("?")))
      case Some(urlWithoutArguments) =>
        Option(urlWithoutArguments)
      case _ =>
        None
    }
  }

  def getFacebookArtistByFacebookUrl(url: String): Future[Option[ArtistWithWeightedGenres]] = {
    normalizeFacebookUrl(url) match {
      case Some(normalizedFacebookUrl) =>
        facebookAPI.getArtist(normalizedFacebookUrl).flatMap(response => readFacebookArtist(response))

      case _ =>
        log("No Facebook artist found for this facebook url")
        Future.successful(None)
    }
  }

 def getFacebookArtistByFacebookUrls(urls: Set[String]): Future[Set[ArtistWithWeightedGenres]] =
   Future.sequence(urls map(url => getFacebookArtistByFacebookUrl(url))).map(_.flatten)

  def splitArtistNamesInTitle(title: String): List[String] = {
    "@.*".r
      .replaceFirstIn(title, "")
      .split("[^\\S].?\\W")
      .toList
      .filter(_ != "") map {
      _.toLowerCase
        .replace("live", "")
        .replace("djset", "")
        .replace("dj set", "")
        .replace("set", "")
        .trim()
    }
  }

  def aggregateImageAndOffset(maybeCover: Option[Cover]): Option[String] = maybeCover match {
    case Some(cover) => Option(cover.source + """\""" + cover.offset_x.toString + """\""" + cover.offset_y.toString)
    case None => None
  }

  def getFacebookArtistsAndReadThem(pattern: String): Future[Seq[ArtistWithWeightedGenres]] =
    facebookAPI.getArtists(pattern) flatMap readFacebookArtists

  def fromFacebookArtistToArtist(facebookArtist: FacebookArtist): Future[Option[ArtistWithWeightedGenres]] = {
    val category = facebookArtist.category

    if (category.equalsIgnoreCase("Musician/Band") || category.equalsIgnoreCase("Artist")) {
      val country: Option[String] = extractCountry(facebookArtist)

      websites.fromWebsiteStringToSet(facebookArtist.website) map { websites =>
        val artist = Artist(
          facebookId = facebookArtist.id,
          name = facebookArtist.name,
          imagePath = aggregateImageAndOffset(facebookArtist.cover),
          description = facebookArtist.description,
          facebookUrl = extractFacebookUrl(facebookArtist.link),
          websites = websites,
          likes = Option(facebookArtist.fan_count),
          country = country)

        Option(ArtistWithWeightedGenres(artist = artist))
      }
    } else {
      Future.successful(None)
    }
  }

  def readFacebookArtist(facebookResponse: JsValue): Future[Option[ArtistWithWeightedGenres]] = {
    facebookResponse.validate[FacebookArtist] match {
      case artist: JsSuccess[FacebookArtist] =>
        fromFacebookArtistToArtist(artist.get)

      case e: JsError =>
        log(facebookResponse.toString() + e)
        Future.successful(None)
    }
  }

  def readFacebookArtists(facebookJsonResponse: JsValue): Future[Seq[ArtistWithWeightedGenres]] = {
    (facebookJsonResponse \ "data").validate[Seq[FacebookArtist]] match {
      case artists: JsSuccess[Seq[FacebookArtist]] =>
        val filteredArtists = artists
          .get
          .map(fromFacebookArtistToArtist)
        Future.sequence(filteredArtists) map(_.flatten)

      case e: JsError =>
        log(e)
        Future.successful(Seq.empty)
    }
  }

  def extractCountry(facebookArtist: FacebookArtist): Option[String] = facebookArtist.location match {
    case Some(location) => location.country
    case None => None
  }
}
