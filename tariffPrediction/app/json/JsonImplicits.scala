package json

import application.PredictionLabels
import models._
import play.api.libs.json.{JsResult, JsSuccess, JsValue, _}

trait JsonImplicits {
  implicit object CharWrites extends Writes[Char] {
    def writes(char: Char): JsString = JsString(char.toString)
  }

  implicit object CharReads extends Reads[Char] {
    def reads(char: JsValue): JsResult[Char] = JsSuccess(Json.stringify(char)(1))
  }
  implicit val genreFormat = Json.format[Genre]
  implicit val countsFormat: Format[Counts] = Json.format[Counts]
  implicit val artistFormat: Format[Artist] = Json.format[Artist]
  implicit val addressFormat = Json.format[Address]
  implicit val placeFormat = Json.format[Place]
  implicit val placeWithAddressFormat = Json.format[PlaceWithAddress]
  implicit val organizerFormat = Json.format[Organizer]
  implicit val organizerWithAddressFormat = Json.format[OrganizerWithAddress]
  implicit val eventFormat = Json.format[Event]
  implicit val eventWithRelationsFormat = Json.format[EventWithRelations]
  implicit val predictionLabelsFormat = Json.format[PredictionLabels]
}
