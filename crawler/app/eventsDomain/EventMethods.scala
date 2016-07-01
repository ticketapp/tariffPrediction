package eventsDomain

import javax.inject.Inject

import APIs.FacebookAPI.FacebookEvent
import APIs.{FacebookAPI, FormatResponses}
import akka.util.Timeout
import artistsDomain.ArtistMethods
import facebookLimit.FacebookLimit.isFacebookLimitReached
import json.JsonHelper._
import logger.LoggerHelper
import models.{Counts, Event, EventAndPlaceFacebookUrl, EventWithRelations}
import organizersDomain.OrganizerMethods
import placesDomain.PlaceMethods
import play.api.libs.json.Reads._
import play.api.libs.json._
import tariffsDomain.TariffMethods
import websites.Websites

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

class EventMethods @Inject()(organizerMethods: OrganizerMethods,
                             artistMethods: ArtistMethods,
                             tariffMethods: TariffMethods,
                             placeMethods: PlaceMethods,
                             implicit val ec: ExecutionContext,
                             websites: Websites,
                             facebookAPI: FacebookAPI)
    extends LoggerHelper
    with FormatResponses {

  def getFacebookEvent(eventFacebookId: String): Future[Option[EventWithRelations]] = facebookAPI
    .getEvent(eventFacebookId)
    .flatMap(facebookEventToEventWithRelations) recover { case NonFatal(e) =>
      log("Event.findEventOnFacebookByFacebookId", e)
      None
    }

  def getFacebookEventWithoutPlace(placeFacebookUrl: String, facebookId: String): Future[EventAndPlaceFacebookUrl] = {
    facebookAPI.getEvent(facebookId) flatMap { event: JsValue =>
      facebookEventToEventWithRelations(fromJsValueTo[FacebookEvent](event)) map { event =>
        EventAndPlaceFacebookUrl(
          placeFacebookUrl = placeFacebookUrl,
          event = event)
      }
    }
  }

  def getEventsFacebookIdByPlaceOrOrganizerFacebookId(facebookId: String): Future[Set[String]] =
    facebookAPI.getEventsId(facebookId) map readEventsIds

  def facebookEventToEventWithRelations(eventFacebookResponse: JsValue): Future[Option[EventWithRelations]] =
    facebookEventToEventWithRelations(fromJsValueTo[FacebookEvent](eventFacebookResponse)) map Option.apply

  private def facebookEventToEventWithRelations(event: FacebookEvent): Future[EventWithRelations] = {
    implicit val timeout: Timeout = 1.minute

    for {
      ticketSellers <-
        websites.getUnshortedNormalizedWebsites(event.description) map tariffMethods.findTicketSellers
      organizer <- organizerMethods.getOrganizerOnFacebook(event.owner.id)
    } yield {
      EventWithRelations(
        event = Event(
          id = None,
          facebookId = event.id,
          name = refactorEventOrPlaceName(event.name),
          description = event.description,
          startTime = stringToDateTime(event.start_time),
          endTime = optionStringToOptionDateTime(event.end_time),
          imagePath = extractImagePath(event.cover),
          tariffRange = tariffMethods.extractTariffRange(event.description),
          ticketSellers = ticketSellers),
        organizers = Seq(organizer),
        addresses = Vector(extractAddressFromLocation(event.location)).flatten,
        places = Vector(placeMethods.facebookPlaceToPlaceWithAddress(event.place)).flatten,
        counts = Option(Counts(
          eventFacebookId = event.id,
          attending_count = event.attending_count,
          declined_count = event.declined_count,
          interested_count = event.interested_count,
          maybe_count = event.maybe_count,
          noreply_count = event.noreply_count)))
    }
  }

  private def readEventsIds(resp: JsValue): Set[String] = Try {
    (resp \ "data").as[Seq[Option[String]]](Reads.seq((__ \ "id").readNullable[String])).flatten
  } match {
    case Success(facebookIds) =>
      facebookIds.toSet

    case Failure(error) if isFacebookLimitReached(error) =>
      throw new FacebookRequestLimit()

    case _ =>
      log("Invalid event ids from Facebook:" + resp)
      Set.empty
  }
}