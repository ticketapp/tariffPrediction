package eventsDomain

import java.util.UUID
import javax.inject.Inject

import APIs.{FacebookAPI, FormatResponses}
import akka.actor.{Actor, ActorLogging}
import akka.pattern.ask
import akka.util.Timeout
import application.ClaudeAddressActor
import application.ClaudeAddressActor.WhatIsClaudeAddress
import facebookLimit.FacebookLimit
import json.JsonHelper.FacebookRequestLimit
import logger.ActorsLoggerHelper
import play.api.libs.concurrent.InjectedActorSupport
import play.api.libs.ws.WSClient

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

object GetFacebookEventActor {
  final case class GetAndSaveFacebookEvent(eventFacebookId: String)
  final case class GetAndSaveFacebookEventWithoutItsPlace(placeFacebookUrl: String, eventFacebookId: String)
  final case class FacebookRequestLimitReached(boolean: Boolean)
}

class GetFacebookEventActor @Inject() (ws: WSClient,
                                       eventMethods: EventMethods,
                                       implicit val ec: ExecutionContext,
                                       wSClient: WSClient,
                                       facebookAPI: FacebookAPI,
                                       claudeAddressActorFactory: ClaudeAddressActor.Factory)
    extends Actor
      with ActorLogging
      with InjectedActorSupport
      with ActorsLoggerHelper
      with FormatResponses
      with FacebookLimit {
  import GetFacebookEventActor._

  implicit val timeout = Timeout(5.seconds)

  val claudeAddressActor = injectedChild(claudeAddressActorFactory(ec, wSClient), UUID.randomUUID().toString)

  val eventuallyClaudeAddress = claudeAddressActor ? WhatIsClaudeAddress() map(_.asInstanceOf[String])

  val claudeSaveEventActor = eventuallyClaudeAddress map { claudeAddress =>
    context.actorSelection(s"$claudeAddress/user/event-actor")
  }

  def getAndSaveFacebookEvent(facebookId: String): Unit = getFacebookEvent(facebookId) map {
    case Some(event: EventWithRelations) => claudeSaveEventActor map(_ ! event) recover { case NonFatal(e) => logE(e) }
    case _ => log error "getFacebookEvent returned None"
  }

  //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    //Always return false instead of asking Claude
  //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
  def existsWithThisFacebookId(facebookId: String) = Future.successful(false)

  def getFacebookEvent(facebookId: String): Future[Option[EventWithRelations]] = {
    existsWithThisFacebookId(facebookId) flatMap {
      case false => eventMethods.getFacebookEvent(facebookId)
      case true => Future.successful(None)
    }
  }

  def getAndSaveFacebookEventWithoutPlace(placeFacebookUrl: String, facebookId: String): Unit = {
    existsWithThisFacebookId(facebookId) map {
      case false =>
        eventMethods
          .getFacebookEventWithoutPlace(placeFacebookUrl, facebookId)
          .map { event: EventAndPlaceFacebookUrl =>
            claudeSaveEventActor map(_ ! event) recover { case NonFatal(e) => logE(e) }
          }.recover {
            case e: FacebookRequestLimit => informSupervisorOfLimitRequest("events-by-places-supervisor")
            case NonFatal(e) => logE(e)
          }

      case true =>
    }
  }

  def receive: Receive = {
    case getAndSaveFacebookEventWithItsPlace: GetAndSaveFacebookEvent =>
      getAndSaveFacebookEvent(getAndSaveFacebookEventWithItsPlace.eventFacebookId)

    case getAndSaveFacebookEventWithoutItsPlace: GetAndSaveFacebookEventWithoutItsPlace =>
      getAndSaveFacebookEventWithoutPlace(
        getAndSaveFacebookEventWithoutItsPlace.placeFacebookUrl,
        getAndSaveFacebookEventWithoutItsPlace.eventFacebookId)

    case _ =>
      log error "Unhandled Message"
  }
}
