package organizersDomain

import java.util.UUID

import akka.actor.{Actor, ActorLogging}
import akka.pattern.ask
import akka.util.Timeout
import application.ClaudeAddressActor
import application.ClaudeAddressActor.WhatIsClaudeAddress
import com.google.inject.Inject
import json.JsonHelper._
import logger.ActorsLoggerHelper
import play.api.libs.concurrent.InjectedActorSupport
import play.api.libs.json.Json
import play.api.libs.ws.WSClient

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object GetOrganizerActor {
  final case class OrganizerFacebookId(facebookId: String)
}

class GetOrganizerActor @Inject() (organizerMethods: OrganizerMethods,
                                   wSClient: WSClient,
                                   implicit val ec: ExecutionContext,
                                   claudeAddressActorFactory: ClaudeAddressActor.Factory)
    extends Actor with ActorsLoggerHelper with ActorLogging with InjectedActorSupport {
  import GetOrganizerActor._

  implicit val timeout = Timeout(5.seconds)

  val claudeAddressActor = injectedChild(claudeAddressActorFactory(ec, wSClient), UUID.randomUUID().toString)

  val eventuallyClaudeAddress = claudeAddressActor ? WhatIsClaudeAddress() map(_.asInstanceOf[String])

  val claudeOrganizerActor = eventuallyClaudeAddress map { claudeAddress =>
    context.actorSelection(s"$claudeAddress/user/organizer-actor")
  }

  override def receive = {
    case organizerFacebookId: OrganizerFacebookId =>
      organizerMethods.getOrganizerOnFacebook(organizerFacebookId.facebookId) map { organizer =>
        claudeOrganizerActor map(_ ! Json.toJson(organizer))
      }

    case _ =>
      log error "Unhandled message"
  }
}
