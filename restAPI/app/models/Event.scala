package models

import com.vividsolutions.jts.geom.{Coordinate, Geometry, GeometryFactory}
import org.joda.time.DateTime

import scala.language.postfixOps

final case class EventWithRelations(event: Event,
                                    organizers: Seq[OrganizerWithAddress] = Vector.empty,
                                    artists: Seq[Artist] = Vector.empty,
                                    places: Seq[PlaceWithAddress] = Vector.empty,
                                    addresses: Seq[Address] = Vector.empty,
                                    counts: Option[Counts] = None)

final case class Event(id: Option[Long] = None,
                       facebookId: Option[String] = None,
                       name: String,
                       description: Option[String] = None,
                       startTime: DateTime,
                       endTime: Option[DateTime] = None,
                       ageRestriction: Int = 16,
                       tariffRange: Option[String] = None,
                       ticketSellers: Option[String] = None,
                       imagePath: Option[String] = None)

final case class Address(id: Option[Long] = None,
                         city: Option[String] = None,
                         zip: Option[String] = None,
                         street: Option[String] = None,
                         country: Option[String] = None)

final case class Place(id: Option[Long] = None,
                       name: String,
                       facebookId: Option[String] = None,
                       description: Option[String] = None,
                       websites: Option[String] = None,
                       capacity: Option[Int] = None,
                       openingHours: Option[String] = None,
                       imagePath: Option[String] = None,
                       addressId: Option[Long] = None,
                       linkedOrganizerId: Option[Long] = None,
                       likes: Option[Long] = None)

final case class PlaceWithAddress(place: Place, maybeAddress: Option[Address] = None)

final case class Organizer(id: Option[Long] = None,
                           facebookId: Option[String] = None,
                           name: String,
                           description: Option[String] = None,
                           addressId: Option[Long] = None,
                           phone: Option[String] = None,
                           publicTransit: Option[String] = None,
                           websites: Option[String] = None,
                           verified: Boolean = false,
                           imagePath: Option[String] = None,
                           linkedPlaceId: Option[Long] = None,
                           likes: Option[Long] = None)

final case class OrganizerWithAddress(organizer: Organizer, maybeAddress: Option[Address] = None)

final case class Artist(id: Option[Long] = None,
                        facebookId: Option[String] = None,
                        name: String,
                        imagePath: Option[String] = None,
                        description: Option[String] = None,
                        facebookUrl: String,
                        websites: Set[String] = Set.empty,
                        hasTracks: Boolean = false,
                        likes: Option[Long] = None,
                        country: Option[String] = None)

final case class Counts(eventFacebookId: String,
                        attending_count: Long,
                        declined_count : Long,
                        interested_count: Long,
                        maybe_count: Long,
                        noreply_count: Long)
