package models

import play.api.libs.json.{Format, Json}

trait jsonImplicits {
  implicit val countsFormat: Format[Counts] = Json.format[Counts]
  implicit val artistFormat: Format[Artist] = Json.format[Artist]
  implicit val addressFormat = Json.format[Address]
  implicit val placeFormat = Json.format[Place]
  implicit val placeWithAddressFormat = Json.format[PlaceWithAddress]
  implicit val organizerFormat = Json.format[Organizer]
  implicit val organizerWithAddressFormat = Json.format[OrganizerWithAddress]
  implicit val eventFormat = Json.format[Event]
  implicit val eventWithRelationsFormat = Json.format[EventWithRelations]
}
