package services

import java.text.Normalizer

import com.vividsolutions.jts.geom.{Coordinate, GeometryFactory}
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.libs.json._

import scala.collection.mutable.ListBuffer

trait Utilities {
  val googleKey = "AIzaSyAnSP6SxPaPYnwmSj-xVuQdGfzmp6DSZ94"
  val echonestApiKey = "3ZYZKU3H3MKR2M59Z"

  val geographicPointPattern = """(-?\(\d+\.?\d*,-?\d+\.?\d*\))"""

  val UNIQUE_VIOLATION = "23505"
  val FOREIGN_KEY_VIOLATION = "23503"

  val antarcticPoint = new GeometryFactory().createPoint(new Coordinate(-84, 30))

  def replaceAccentuatedLetters(string: String): String =
    Normalizer.normalize(string, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "")

  def stripChars(s:String, ch:String)= s filterNot (ch contains _)

  def optionStringToLowerCaseOptionString(maybeString: Option[String]): Option[String] = maybeString match {
    case Some(string: String) => Option(string.toLowerCase)
    case None => None
  }

  def removeSpecialCharacters(string: String): String = string.replaceAll("""[*ù$-+/*_\.\\,#'~´&]""", "")

  def phoneNumbersStringToSet(phoneNumbers: Option[String]): Set[String] = phoneNumbers match {
    case None => Set.empty
    case Some(phoneNumbersValue: String) =>
      def normalizePhoneNumberPrefix(phoneNumber: String): String = phoneNumber match {
        case phoneNumberStartsWith0033 if phoneNumberStartsWith0033.startsWith("0033") =>
          "0" + phoneNumber.drop(4)
        case phoneNumberStartsWith0033 if phoneNumberStartsWith0033.startsWith("+0033") =>
          "0" + phoneNumber.drop(5)
        case phoneNumberStartsWith33 if phoneNumberStartsWith33.startsWith("33") =>
          "0" + phoneNumber.drop(2)
        case phoneNumberStartsWithPlus33 if phoneNumberStartsWithPlus33.startsWith("+33") =>
          "0" + phoneNumber.drop(3)
        case alreadyNormalized: String =>
          alreadyNormalized
        case _ => ""
      }

      var numberWithoutLetters = phoneNumbersValue.replaceAll("[^0-9+]", "")
      var normalizedNumbers = ListBuffer.empty[String]

      while(numberWithoutLetters.length >= 10) {
        val withNormalizedPrefix = normalizePhoneNumberPrefix(numberWithoutLetters)
        normalizedNumbers += withNormalizedPrefix.take(10)
        numberWithoutLetters = withNormalizedPrefix.drop(10)
      }
      normalizedNumbers.toSet.filter(_ == "")
  }

  def phoneNumbersSetToOptionString(phoneNumbers: Set[String]): Option[String] = phoneNumbers match {
    case emptySet: Set[String] if emptySet.isEmpty => None
    case phoneNumbersFound => Option(phoneNumbersFound.mkString(","))
  }

  def returnNumberOfHoursBetween4AMAndNow(hoursSinceMidnight: Int): Int = 4 - hoursSinceMidnight match {
    case positive if positive >= 0 => positive
    case negative if negative < 0 => 24 + negative
  }

  def returnMaybeNextPage(facebookResponse: JsValue, objectToGetKey: String): Option[String] = {
    val readNextFacebookPages: Reads[Option[String]] = (__ \ "next").readNullable[String]
    facebookResponse \ objectToGetKey match {
      case JsDefined(objectFound) =>
        (facebookResponse \ objectToGetKey \ "paging").asOpt[Option[String]](readNextFacebookPages).flatten
      case _ =>
        (facebookResponse \ "paging").asOpt[Option[String]](readNextFacebookPages).flatten
    }
  }
}
