package placesDomain

import javax.inject.Inject

import akka.actor.{Actor, ActorLogging, PoisonPill}
import akka.pattern.ask
import application.IsFinished
import com.google.inject.assistedinject.Assisted
import eventsDomain.EventMethods
import eventsDomain.GetFacebookEventActor.GetAndSaveFacebookEventWithoutItsPlace
import facebookLimit.FacebookLimit
import json.JsonHelper._
import placesDomain.EventsByPlacesSupervisor.IncrementCounter
import play.api.libs.concurrent.InjectedActorSupport
import play.api.libs.ws.WSClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

object GetEventsByPlacesActor {
  trait Factory {
    def apply(ec: ExecutionContext,
              eventMethods: EventMethods,
              placeMethods: PlaceMethods,
              wSClient: WSClient,
              claudeAddress: String): Actor
  }
}

class GetEventsByPlacesActor @Inject()(implicit val ec: ExecutionContext,
                                       eventMethods: EventMethods,
                                       placeMethods: PlaceMethods,
                                       wSClient: WSClient,
                                       @Assisted claudeAddress: String)
    extends Actor with ActorLogging with InjectedActorSupport with FacebookLimit {

  implicit val timeout: akka.util.Timeout = 10.seconds

  val eventuallyGetFacebookEventActor = context
    .actorSelection("/user/get-facebook-event")
    .resolveOne()

  val eventuallyClaudePlaceActor = context
    .actorSelection(s"$claudeAddress/user/place-actor")
    .resolveOne()

  startToFindEventsForPlace(0)

  val supervisorPath: String = "/user/events-by-places-supervisor"

  def startToFindEventsForPlace(placeOffset: Long): Unit = findEventsForPlace(placeOffset) map {
    case IsFinished(true) =>
      context
        .actorSelection(supervisorPath)
        .resolveOne()
        .map { actorRef =>
          actorRef ! IsFinished(true)
          self ! PoisonPill
        }
        .recover { case e => log error e.getMessage }

    case IsFinished(false) =>
      context
        .actorSelection(supervisorPath)
        .resolveOne()
        .map(actorRef => actorRef ! IncrementCounter)
        .recover { case e => log error e.getMessage }
  }

  def findEventsForPlace(offset: Long = 0): Future[IsFinished] = {
    val claudePlaceActor = eventuallyClaudePlaceActor.recover { case NonFatal(e) =>
      log error "ClaudePlaceActor does not exist\n" + e
      throw new Exception("ClaudePlaceActor does not exist")
    }

    claudePlaceActor.flatMap { actorRef =>
      val eventuallyPlacesWithAddress: Future[Seq[PlaceWithAddress]] = (actorRef ? offset).mapTo[Seq[PlaceWithAddress]]

      eventuallyPlacesWithAddress flatMap { placesWithAddress =>
        placesWithAddress.headOption match {
          case Some(place) => getAndSaveFacebookEvents(place.place.facebookUrl, place.place.facebookId)
          case None => Future.successful(IsFinished(true))
        }
      }
    }
  }

  def getAndSaveFacebookEvents(placeFacebookUrl: String, placeFacebookId: String): Future[IsFinished] = {
    val eventuallyFacebookIds = eventMethods.getEventsFacebookIdByPlaceOrOrganizerFacebookId(placeFacebookId)

    val unitResults = eventuallyGetFacebookEventActor flatMap { getFacebookEventActor =>
      eventuallyFacebookIds
        .map { facebookIds =>
          facebookIds foreach { facebookId =>
            getFacebookEventActor ! GetAndSaveFacebookEventWithoutItsPlace(placeFacebookUrl, facebookId)
          }
        }
        .recover { case e: FacebookRequestLimit => informSupervisorOfLimitRequest("events-by-places-supervisor") }
    }

    unitResults map(_ => IsFinished(false))
  }

  def receive: Receive = {
    case placeOffset: Long => startToFindEventsForPlace(placeOffset)
    case e: Exception      => throw e
    case _                 => log error "Unhandled message"
  }
}
