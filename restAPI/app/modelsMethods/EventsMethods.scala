package modelsMethods

import javax.inject.Inject

import database.MyPostgresDriver.api._
import database.{EventAddressRelation, _}
import models._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps

class EventsMethods @Inject()(protected val dbConfigProvider: DatabaseConfigProvider,
                          artistMethods: ArtistMethods,
                          organizerMethods: OrganizerMethods,
                          placeMethods: PlaceMethods,
                          attendeeMethods: AttendeeMethods)
    extends HasDatabaseConfigProvider[MyPostgresDriver] with MyDBTableDefinitions {

//  def find(facebookId: String): Future[Option[EventWithRelations]] = {
//    val query = for {
//      ((((eventWithOptionalEventOrganizers), optionalEventArtists), optionalEventPlaces), optionalEventAddresses) <-
//        events.filter(_.facebookId === facebookId) joinLeft
//          (eventsOrganizers join organizers on (_.organizerUrl === _.facebookUrl)) on
//            (_.facebookId === _._1.eventId) joinLeft
//          (eventsArtists join artists on (_.artistId === _.facebookId)) on (_._1.facebookId === _._1.eventId) joinLeft
//          (eventsPlaces join places on (_.placeFacebookUrl === _.facebookUrl)) on
//            (_._1._1.facebookId === _._1.eventId) joinLeft
//          (eventsAddresses join addresses on (_.addressId === _.id)) on (_._1._1._1.facebookId === _._1.eventId) joinLeft
//          eventsCounts on (_._1._1._1._1.facebookId === _.eventFacebookId)
//    } yield (eventWithOptionalEventOrganizers, optionalEventArtists, optionalEventPlaces, optionalEventAddresses)
//
//    db.run(query.result) map(eventWithRelations =>
//      eventWithRelationsTupleToEventWithRelation(eventWithRelations)) map(_.headOption)
//  }

  def find(facebookId: String): Future[Option[EventWithRelations]] = {
    val query = for {
      (((((eventWithOptionalEventOrganizers), optionalEventArtists), optionalEventPlaces), optionalEventGenres),
      optionalEventAddresses) <- events.filter(_.facebookId === facebookId) joinLeft
        (eventsOrganizers join organizers on (_.organizerUrl === _.facebookUrl)) on
        (_.facebookId === _._1.eventId) joinLeft
        (eventsArtists join artists on (_.artistId === _.facebookId)) on (_._1.facebookId === _._1.eventId) joinLeft
        (eventsPlaces join places on (_.placeFacebookUrl === _.facebookUrl)) on
        (_._1._1.facebookId === _._1.eventId) joinLeft
        (eventsGenres join genres on (_.genreId === _.id)) on (_._1._1._1.facebookId === _._1.eventId) joinLeft
        (eventsAddresses join addresses on (_.addressId === _.id)) on (_._1._1._1._1.facebookId === _._1.eventId) joinLeft
        eventsCounts on (_._1._1._1._1._1.facebookId === _.eventFacebookId)
    } yield (eventWithOptionalEventOrganizers, optionalEventArtists, optionalEventPlaces, optionalEventGenres,
      optionalEventAddresses)

    db.run(query.result)
      .map(eventWithRelations => eventWithRelationsTupleToEventWithRelation(eventWithRelations))
      .map(_.headOption)
  }

  def findSinceOffset(offset: Long, numberToReturn: Long): Future[Seq[EventWithRelations]] = {
    val query = for {
      (((((eventWithOptionalEventOrganizers), optionalEventArtists), optionalEventPlaces), optionalEventGenres),
      optionalEventAddresses) <- events.drop(offset).take(numberToReturn) joinLeft
        (eventsOrganizers join organizers on (_.organizerUrl === _.facebookUrl)) on
          (_.facebookId === _._1.eventId) joinLeft
        (eventsArtists join artists on (_.artistId === _.facebookId)) on (_._1.facebookId === _._1.eventId) joinLeft
        (eventsPlaces join places on (_.placeFacebookUrl === _.facebookUrl)) on
        (_._1._1.facebookId === _._1.eventId) joinLeft
          (eventsGenres join genres on (_.genreId === _.id)) on (_._1._1._1.facebookId === _._1.eventId) joinLeft
        (eventsAddresses join addresses on (_.addressId === _.id)) on (_._1._1._1._1.facebookId === _._1.eventId) joinLeft
        eventsCounts on (_._1._1._1._1._1.facebookId === _.eventFacebookId)
    } yield (eventWithOptionalEventOrganizers, optionalEventArtists, optionalEventPlaces, optionalEventGenres,
      optionalEventAddresses)

    db.run(query.result) map(eventWithRelations => eventWithRelationsTupleToEventWithRelation(eventWithRelations))
  }

  def save(eventWithRelations: EventWithRelations): Future[Event] = {
    val query = for {
      eventFound <- events.filter(_.facebookId === eventWithRelations.event.facebookId).result.headOption
      result <- eventFound.map(DBIO.successful).getOrElse(events returning events.map(_.id) += eventWithRelations.event)
    } yield result

    db.run(query) flatMap {
      case e: Event =>
        saveEventRelations(eventWithRelations.copy(event = e))

      case id: Long =>
        saveEventRelations(eventWithRelations.copy(event = eventWithRelations.event.copy(id = Option(id))))
    }
  }

  def saveEventRelations(eventWithRelations: EventWithRelations): Future[Event] = {
    val eventFacebookId: String = eventWithRelations.event.facebookId

    val eventuallyArtistsResult = Future.sequence(eventWithRelations.artists map { artist =>
      artistMethods.saveWithEventRelation(artist, eventFacebookId)
    })
    val eventuallyOrganizersResult = Future.sequence(
      eventWithRelations.organizers map(organizer =>
        organizerMethods.saveWithEventRelation(organizer.organizer, eventFacebookId)))
    val eventuallyPlacesResult =
      Future.sequence(eventWithRelations.places map(place => placeMethods.saveWithEventRelation(place, eventFacebookId)))
    val eventuallyCountsResult = eventWithRelations.counts match {
      case Some(counts) => attendeeMethods.saveCounts(counts)
      case _ => Future.successful((): Unit)
    }

    val results = for {
      artistsResult <- eventuallyArtistsResult
      organizersResult <- eventuallyOrganizersResult
      placesResult <- eventuallyPlacesResult
      countsResult <- eventuallyCountsResult
    } yield (artistsResult, organizersResult, placesResult, countsResult)

    results map( _ => eventWithRelations.event)
  }

  def eventWithRelationsTupleToEventWithRelation(eventWithRelations: Seq[(((Event, Option[(EventOrganizerRelation, Organizer)]),
    Option[(EventArtistRelation, Artist)]), Option[(EventPlaceRelation, Place)], Option[(EventGenreRelation, Genre)],
    Option[(EventAddressRelation, Address)], Option[Counts])]): Vector[EventWithRelations] = {
    val groupedByEvents = eventWithRelations.groupBy(_._1._1)

    groupedByEvents.map { eventWithOptionalRelations =>
      val event = eventWithOptionalRelations._1._1
      val relations = eventWithOptionalRelations._2
      val organizers = (relations collect {
        case (((_, Some((_, organizer: Organizer))), _), _, _, _, _) => organizer
      }).distinct
      val artists = (relations collect {
        case (((_, _), Some((_, artist: Artist))), _, _, _, _) => artist
      }).distinct
      val places = (relations collect {
        case (((_, _), _), Some((_, place: Place)), _, _, _) => place
      }).distinct
      val genres = (relations collect {
        case (((_, _), _), _, Some((_, genre: Genre)), _, _) => genre
      }).distinct
      val addresses = (relations collect {
        case (((_, _), _), _, _, Some((_, address: Address)), _) => address
      }).distinct
      val counts = (relations collect {
        case (((_, _), _), _, _, _, Some(counts: Counts)) => counts
      }).headOption

      EventWithRelations(
        event,
        organizers map (OrganizerWithAddress(_)),
        artists,
        places map (PlaceWithAddress(_)),
        genres,
        addresses,
        counts)
    }.toVector
  }

//  private def eventWithRelationsTupleToEventWithRelation(eventWithRelations:
//                                                         Seq[(((Event, Option[(EventOrganizerRelation, Organizer)]),
//                                                           Option[(EventArtistRelation, Artist)]),
//                                                           Option[(EventPlaceRelation, Place)],
//                                                           Option[(EventAddressRelation, Address)],
//                                                           Option[Counts])]): Vector[EventWithRelations] = {
//    val groupedByEvents = eventWithRelations.groupBy(_._1._1)
//
//    groupedByEvents.map { eventWithOptionalRelations =>
//      val event = eventWithOptionalRelations._1._1
//      val relations = eventWithOptionalRelations._2
//      val organizers = (relations collect {
//        case (((_, Some((_, organizer: Organizer))), _), _, _, _) => organizer
//      }).distinct
//      val artists = (relations collect {
//        case (((_, _), Some((_, artist: Artist))), _, _, _) => artist
//      }).distinct
//      val places = (relations collect {
//        case (((_, _), _), Some((_, place: Place)), _, _) => place
//      }).distinct
//      val addresses = (relations collect {
//        case (((_, _), _), _, Some((_, address: Address)), _) => address
//      }).distinct
//      val counts = (relations collect {
//        case (((_, _), _), _, _, Some(counts: Counts)) => counts
//      }).headOption
//
//      EventWithRelations(
//        event,
//        organizers map (OrganizerWithAddress(_)),
//        artists,
//        places map (PlaceWithAddress(_)),
//        addresses,
//        counts)
//    }.toVector
//  }
}
