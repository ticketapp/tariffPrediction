package placesDomain

import javax.inject.Inject

import akka.actor.{Actor, ActorLogging, PoisonPill}
import com.google.inject.assistedinject.Assisted
import logger.ActorsLoggerHelper
import play.api.libs.concurrent.InjectedActorSupport
import play.api.libs.ws.WSClient
import akka.pattern.ask
import application.MainSupervisor.IsFinished
import facebookLimit.FacebookLimit
import json.JsonHelper.FacebookRequestLimit
import models.{GetPlace, PlaceWithAddress, UpdatePlace}
import org.joda.time.DateTime
import placesDomain.UpdatePlacesSupervisor.IncrementCounter

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

object UpdatePlacesActor {
  trait Factory {
    def apply(placeMethods: PlaceMethods,
              wSClient: WSClient,
              @Assisted claudeAddress: String,
              ec: ExecutionContext): Actor
  }
}

class UpdatePlacesActor @Inject()(implicit val ec: ExecutionContext,
                                  placeMethods: PlaceMethods,
                                  wSClient: WSClient,
                                  @Assisted claudeAddress: String)
    extends Actor with ActorsLoggerHelper with ActorLogging  with InjectedActorSupport with FacebookLimit {

  implicit val timeout: akka.util.Timeout = 10.seconds

  val claudePlaceActor = context
    .actorSelection(s"$claudeAddress/user/place-actor")
    .resolveOne()

  val notUpdatedSince = new DateTime()

  startToUpdatePlace(0)

  val supervisorPath: String = "/user/update-places-main-supervisor"

  def startToUpdatePlace(placeOffset: Long): Unit = updatePlace(placeOffset) map {
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

  def updatePlace(offset: Long = 0): Future[IsFinished] = {
    claudePlaceActor recover { case NonFatal(e) =>
      logE("claudePlaceActor does not exist\n" + e)
      IsFinished(true)
    }
    claudePlaceActor
      .flatMap { actorRef =>
        val eventuallyPlacesWithAddress: Future[Seq[PlaceWithAddress]] =
          (actorRef ? GetPlace(offset, notUpdatedSince)).mapTo[Seq[PlaceWithAddress]]

        eventuallyPlacesWithAddress flatMap { placesWithAddress =>
          placesWithAddress.headOption match {
            case Some(place) =>
              placeMethods
                .getPlace(place.place.facebookId)
                .flatMap { facebookPlace =>
                  actorRef ! UpdatePlace(facebookPlace.copy(facebookPlace.place.copy(id = place.place.id)))
                  Future.successful(IsFinished(false))
                }
                .recover { case e: FacebookRequestLimit =>
                  informSupervisorOfLimitRequest("update-places-supervisor")
                  IsFinished(false)
                }

            case None =>
              Future.successful(IsFinished(true))
          }
        }
      }.recover { case NonFatal(e) =>
      logE("An exception has been thrown\n" + e)
      IsFinished(false)
    }
  }

  def receive: Receive = {
    case placeOffset: Long => startToUpdatePlace(placeOffset)
    case e: Exception      => throw e
    case _                 => log error "Unhandled message"
  }
}