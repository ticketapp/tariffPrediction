package models

import org.joda.time.DateTime

import scala.language.postfixOps

final case class EventWithRelations(event: Event,
                                    organizers: Seq[OrganizerWithAddress] = Vector.empty,
                                    artists: Seq[Artist] = Vector.empty,
                                    places: Seq[PlaceWithAddress] = Vector.empty,
                                    genres: Seq[Genre] = Vector.empty,
                                    addresses: Seq[Address] = Vector.empty,
                                    counts: Option[Counts] = None)

final case class Event(id: Option[Long] = None,
                       facebookId: String,
                       name: String,
                       description: Option[String] = None,
                       startTime: DateTime,
                       endTime: Option[DateTime] = None,
                       ageRestriction: Int = 16,
                       tariffRange: Option[String] = None,
                       ticketSellers: Option[String] = None,
                       imagePath: Option[String] = None)

@SerialVersionUID(42L)
final case class EventAndPlaceFacebookUrl(event: EventWithRelations, placeFacebookUrl: String)

final case class Address(id: Option[Long] = None,
                         city: Option[String] = None,
                         zip: Option[String] = None,
                         street: Option[String] = None,
                         country: Option[String] = None)

final case class Place(id: Option[Long] = None,
                       name: String,
                       facebookId: String,
                       facebookUrl: String,
                       description: Option[String] = None,
                       websites: Option[String] = None,
                       capacity: Option[Int] = None,
                       openingHours: Option[String] = None,
                       imagePath: Option[String] = None,
                       addressId: Option[Long] = None,
                       linkedOrganizerUrl: Option[String] = None,
                       likes: Option[Long] = None)

@SerialVersionUID(42L)
final case class PlaceWithAddress(place: Place, maybeAddress: Option[Address] = None)

@SerialVersionUID(42L)
final case class UpdatePlace(place: PlaceWithAddress)

@SerialVersionUID(42L)
final case class GetPlace(offset: Long, notUpdatedSince: DateTime)

final case class Organizer(id: Option[Long] = None,
                           facebookId: String,
                           facebookUrl: String,
                           name: String,
                           description: Option[String] = None,
                           addressId: Option[Long] = None,
                           phone: Option[String] = None,
                           publicTransit: Option[String] = None,
                           websites: Option[String] = None,
                           verified: Boolean = false,
                           imagePath: Option[String] = None,
                           linkedPlaceUrl: Option[String] = None,
                           likes: Option[Long] = None)

final case class OrganizerWithAddress(organizer: Organizer, maybeAddress: Option[Address] = None)

final case class Artist(facebookId: String,
                        name: String,
                        imagePath: Option[String] = None,
                        description: Option[String] = None,
                        facebookUrl: String,
                        websites: Set[String] = Set.empty,
                        hasTracks: Boolean = false,
                        likes: Option[Long] = None,
                        country: Option[String] = None)

final case class ArtistWithWeightedGenres(artist: Artist, genres: Seq[GenreWithWeight] = Seq.empty)

@SerialVersionUID(42L)
final case class EventIdArtistsAndGenres(eventId: String,
                                         artistsWithWeightedGenres: Seq[ArtistWithWeightedGenres],
                                         genresWithWeight: Seq[Genre])

case class Genre(id: Option[Int] = None, name: String, icon: Char = 'a') {
  require(name.nonEmpty, "It is forbidden to create a genre without a name.")
}

case class GenreWithWeight(genre: Genre, weight: Int = 1)

final case class Counts(eventFacebookId: String,
                        attending_count: Long,
                        declined_count : Long,
                        interested_count: Long,
                        maybe_count: Long,
                        noreply_count: Long)