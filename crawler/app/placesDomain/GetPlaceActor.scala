package placesDomain

import java.util.UUID

import akka.actor.{Actor, ActorLogging}
import akka.pattern.ask
import akka.util.Timeout
import application.ClaudeAddressActor
import application.ClaudeAddressActor.WhatIsClaudeAddress
import com.google.inject.Inject
import logger.ActorsLoggerHelper
import play.api.libs.concurrent.InjectedActorSupport
import play.api.libs.ws.WSClient

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object GetPlaceActor {
  final case class PlaceFacebookId(facebookId: String)
}

class GetPlaceActor @Inject() (placeMethods: PlaceMethods,
                               wSClient: WSClient,
                               implicit val ec: ExecutionContext,
                               claudeAddressActorFactory: ClaudeAddressActor.Factory)
    extends Actor with ActorsLoggerHelper with ActorLogging with InjectedActorSupport {
  import GetPlaceActor._

  implicit val timeout = Timeout(5.seconds)

  val claudeAddressActor = injectedChild(claudeAddressActorFactory(ec, wSClient), UUID.randomUUID().toString)

  val eventuallyClaudeAddress = claudeAddressActor ? WhatIsClaudeAddress() map(_.asInstanceOf[String])

  val claudePlaceActor = eventuallyClaudeAddress map { claudeAddress =>
    context.actorSelection(s"$claudeAddress/user/place-actor")
  }

  override def receive: Receive = {
    case placeFacebookId: PlaceFacebookId =>
      placeMethods.getPlace(placeFacebookId.facebookId) map(place => claudePlaceActor map(_ ! place))

    case _ =>
      log error "Unhandled message"
  }
}
