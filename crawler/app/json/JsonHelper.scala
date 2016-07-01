package json

import java.sql.Timestamp

import com.vividsolutions.jts.geom.Geometry
import com.vividsolutions.jts.io.{WKTReader, WKTWriter}
import logger.LoggerHelper
import models._
import play.api.Logger
import play.api.libs.json.{JsNumber, _}

object JsonHelper extends LoggerHelper {
  final case class FacebookRequestLimit() extends Exception {
    Logger.info("Facebook request limit reached")
  }

  def fromJsValueTo[A](jsValue: JsValue)(implicit deserializer: OFormat[A]): A = jsValue.validate[A] match {
    case success: JsSuccess[A] =>
      success.get

    case e: JsError =>
      if (jsValue.toString.contains("Application request limit reached")) {
        throw new FacebookRequestLimit
      } else {
        log(e)
        log(jsValue.toString)
        throw new JsResultException(e.errors)
      }
  }

  implicit object JavaBigDecimalWrites extends Writes[java.math.BigDecimal] {
    def writes(bigDecimal: java.math.BigDecimal): JsNumber = JsNumber(BigDecimal(bigDecimal))
  }

  implicit object CharWrites extends Writes[Char] {
    def writes(char: Char): JsString = JsString(char.toString)
  }

  implicit object CharReads extends Reads[Char] {
    def reads(char: JsValue): JsResult[Char] = JsSuccess(Json.stringify(char)(1))
  }

  implicit object TimestampReads extends Reads[Timestamp] {
    def reads(char: JsValue): JsResult[Timestamp] = JsSuccess(new Timestamp(Json.stringify(char).toLong))
  }

  def geomJsonFormat[G <: Geometry]: Format[G] = Format[G](
    fjs = Reads.StringReads.map(fromWKT[G]),
    tjs = new Writes[G] {
      def writes(o: G): JsValue = JsString(toWKT(o))
    }
  )

  private val wktWriterHolder = new ThreadLocal[WKTWriter]
  private val wktReaderHolder = new ThreadLocal[WKTReader]

  private def toWKT(geom: Geometry): String = {
    if (wktWriterHolder.get == null) wktWriterHolder.set(new WKTWriter())
    wktWriterHolder.get.write(geom)
  }

  private def fromWKT[T](wkt: String): T = {
    if (wktReaderHolder.get == null) wktReaderHolder.set(new WKTReader())
    wktReaderHolder.get.read(wkt).asInstanceOf[T]
  }

  implicit val geometryJsonFormat = geomJsonFormat[Geometry]

  def enumReads[E <: Enumeration](enum: E): Reads[E#Value] = new Reads[E#Value] {
    def reads(json: JsValue): JsResult[E#Value] = json match {
      case JsString(s) =>
        try JsSuccess(enum.withName(s)) catch { case _: NoSuchElementException =>
          JsError(s"Enumeration expected of type: '${enum.getClass}', but it does not appear to contain the value: '$s'")
        }

      case _ =>
        JsError("String value expected")
    }
  }

  implicit def enumWrites[E <: Enumeration]: Writes[E#Value] = new Writes[E#Value] {
    def writes(v: E#Value): JsValue = JsString(v.toString)
  }

  implicit val genreFormat = Json.format[Genre]
  implicit val artistFormat: Format[Artist] = Json.format[Artist]
  implicit val genreWithWeightFormat = Json.format[GenreWithWeight]
  implicit val artistWithGenresFormat = Json.format[ArtistWithWeightedGenres]
  implicit val addressFormat = Json.format[Address]
  implicit val placeFormat = Json.format[Place]
  implicit val countsFormat: Format[Counts] = Json.format[Counts]
  implicit val placeWithAddressFormat = Json.format[PlaceWithAddress]
  implicit val organizerFormat = Json.format[Organizer]
  implicit val organizerWithAddressFormat = Json.format[OrganizerWithAddress]
  implicit val eventFormat = Json.format[Event]
  implicit val eventWithRelationsFormat = Json.format[EventWithRelations]
  implicit val eventAndPlaceFacebookIdFormat: Format[EventAndPlaceFacebookUrl] = Json.format[EventAndPlaceFacebookUrl]
}
