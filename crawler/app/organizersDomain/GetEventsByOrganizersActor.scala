package organizersDomain

import javax.inject.Inject

import akka.actor.{Actor, ActorLogging}
import akka.pattern.ask
import eventsDomain.GetFacebookEventActor
import GetFacebookEventActor.GetAndSaveFacebookEvent
import application.IsFinished
import com.google.inject.assistedinject.Assisted
import eventsDomain.EventMethods
import facebookLimit.FacebookLimit
import organizersDomain.EventsByOrganizersSupervisor._
import play.api.libs.json.JsValue

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import json.JsonHelper._

object GetEventsByOrganizersActor {
  trait Factory {
    def apply(organizerMethods: OrganizerMethods, eventMethods: EventMethods, claudeAddress: String): Actor
  }
}

class GetEventsByOrganizersActor @Inject()(organizerMethods: OrganizerMethods,
                                           eventMethods: EventMethods,
                                           implicit val ec: ExecutionContext,
                                           @Assisted claudeAddress: String)
    extends Actor with ActorLogging with FacebookLimit {

  implicit val timeout: akka.util.Timeout = 1.seconds

  val eventuallyGetFacebookEventActor = context
    .actorSelection("/user/get-facebook-event")
    .resolveOne()

  val claudeOrganizerActor = context
    .actorSelection(s"$claudeAddress/user/organizer-actor")
    .resolveOne()

  startToFindEventsForOrganizer(0)

  def startToFindEventsForOrganizer(organizerOffset: Long): Unit = findEventsForOrganizer(organizerOffset) map {
    case IsFinished(true) =>
      context
        .actorSelection("/user/events-by-organizers-supervisor")
        .resolveOne()
        .map(actorRef => actorRef ! IsFinished(true))
        .recover { case e => log error e.getMessage }

    case IsFinished(false) =>
      context
        .actorSelection("/user/events-by-organizers-supervisor")
        .resolveOne()
        .map(actorRef => actorRef ! IncrementCounter)
        .recover { case e => log error e.getMessage }
  }

  def findEventsForOrganizer(offset: Long = 0): Future[IsFinished] = claudeOrganizerActor.flatMap { actorRef =>
    val eventuallyOrganizersWithAddressJson: Future[JsValue] = (actorRef ? offset).mapTo[JsValue]

    eventuallyOrganizersWithAddressJson flatMap { organizersWithAddressJson =>
      organizersWithAddressJson.validate[Seq[OrganizerWithAddress]].get.headOption match {
        case Some(organizer) => getAndSaveFacebookEvents(organizer.organizer.facebookId)
        case None => Future.successful(IsFinished(true))
      }
    }
  }

  def getAndSaveFacebookEvents(organizerFacebookId: String): Future[IsFinished] = {
    val eventuallyFacebookIds = eventMethods.getEventsFacebookIdByPlaceOrOrganizerFacebookId(organizerFacebookId)

    val unitResults = eventuallyGetFacebookEventActor flatMap { getFacebookEventActor =>
      eventuallyFacebookIds
        .map { facebookIds =>
          facebookIds foreach { facebookId =>
            getFacebookEventActor ! GetAndSaveFacebookEvent(facebookId)
          }
        }
        .recover { case e: FacebookRequestLimit => informSupervisorOfLimitRequest("events-by-organizers-supervisor") }
    }

    unitResults map(_ => IsFinished(false))
  }

  def receive = {
    case organizerOffset: Long => startToFindEventsForOrganizer(organizerOffset)
    case e: Exception          => throw e
    case _                     => log error "Unhandled message"
  }
}
