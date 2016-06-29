package models

import javax.inject.Inject

import database.MyPostgresDriver.api._
import database.{EventAddressRelation, _}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps

class EventsDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
    extends HasDatabaseConfigProvider[MyPostgresDriver] with MyDBTableDefinitions {

  def find(facebookId: String): Future[Option[EventWithRelations]] = {
    val query = for {
      ((((eventWithOptionalEventOrganizers), optionalEventArtists), optionalEventPlaces), optionalEventAddresses) <-
        events.filter(_.facebookId === facebookId) joinLeft
          (eventsOrganizers join organizers on (_.organizerUrl === _.facebookUrl)) on
            (_.facebookId === _._1.eventId) joinLeft
          (eventsArtists join artists on (_.artistId === _.facebookId)) on (_._1.facebookId === _._1.eventId) joinLeft
          (eventsPlaces join places on (_.placeFacebookUrl === _.facebookUrl)) on
            (_._1._1.facebookId === _._1.eventId) joinLeft
          (eventsAddresses join addresses on (_.addressId === _.id)) on (_._1._1._1.facebookId === _._1.eventId) joinLeft
          eventsCounts on (_._1._1._1._1.facebookId === _.eventFacebookId)
    } yield (eventWithOptionalEventOrganizers, optionalEventArtists, optionalEventPlaces, optionalEventAddresses)

    db.run(query.result) map(eventWithRelations =>
      eventWithRelationsTupleToEventWithRelation(eventWithRelations)) map(_.headOption)
  }

  private def eventWithRelationsTupleToEventWithRelation(eventWithRelations:
                                                         Seq[(((Event, Option[(EventOrganizerRelation, Organizer)]),
                                                           Option[(EventArtistRelation, Artist)]),
                                                           Option[(EventPlaceRelation, Place)],
                                                           Option[(EventAddressRelation, Address)],
                                                           Option[Counts])]): Vector[EventWithRelations] = {
    val groupedByEvents = eventWithRelations.groupBy(_._1._1)

    groupedByEvents.map { eventWithOptionalRelations =>
      val event = eventWithOptionalRelations._1._1
      val relations = eventWithOptionalRelations._2
      val organizers = (relations collect {
        case (((_, Some((_, organizer: Organizer))), _), _, _, _) => organizer
      }).distinct
      val artists = (relations collect {
        case (((_, _), Some((_, artist: Artist))), _, _, _) => artist
      }).distinct
      val places = (relations collect {
        case (((_, _), _), Some((_, place: Place)), _, _) => place
      }).distinct
      val addresses = (relations collect {
        case (((_, _), _), _, Some((_, address: Address)), _) => address
      }).distinct
      val counts = (relations collect {
        case (((_, _), _), _, _, Some(counts: Counts)) => counts
      }).headOption

      EventWithRelations(
        event,
        organizers map (OrganizerWithAddress(_)),
        artists,
        places map (PlaceWithAddress(_)),
        addresses,
        counts)
    }.toVector
  }
}
