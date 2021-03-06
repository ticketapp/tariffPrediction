package APIs

import APIs.FacebookAPI.{Cover, Location}
import com.vividsolutions.jts.geom.Geometry
import models.Address
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import websites.Websites._

trait FormatResponses {
  def refactorEventOrPlaceName(eventName: String): String = eventName.indexOf(" @") match {
    case -1 => eventName
    case index => eventName.take(index).trim
  }

  def refactorFacebookLink(link: String): String = link
    .replace("https://www.facebook.com/", "")
    .replaceAll("pages/", "")
    .replaceAll("/\\.*", "")

  def stringToDateTime(string: String): DateTime = {
    val formattedString = string.replace("T", " ").substring(0, string.length - 5)
    DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss").parseDateTime(formattedString)
  }

  def optionStringToOptionDateTime(maybeDate: Option[String]): Option[DateTime] = maybeDate match {
    case Some(date) => Option(stringToDateTime(date))
    case None => None
  }

  def extractFacebookUrl(link: String): String =
    normalizeUrl(link).substring("facebook.com/".length).replace("pages/", "").replace("/", "")

  def extractAddressFromLocation(maybeLocation: Option[Location]): Option[Address] = maybeLocation match {
    case Some(location) =>
      Option(Address(
        city = location.city,
        zip = location.zip,
        street = location.street,
        country = location.country))

    case _ =>
      None
  }

  def extractGeographicPointFromLocation(maybeLocation: Option[Location]): Option[Geometry] = None

  def extractImagePath(maybeCover: Option[Cover]): Option[String] = maybeCover match {
    case Some(cover) => Option(cover.source)
    case _ => None
  }
}
