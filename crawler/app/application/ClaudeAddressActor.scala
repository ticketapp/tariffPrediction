package application

import javax.inject.Inject

import akka.actor.{PoisonPill, Actor, ActorLogging}
import application.ClaudeAddressActor.WhatIsClaudeAddress
import logger.ActorsLoggerHelper
import play.api.libs.ws.WSClient

import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal
import scala.concurrent.duration._

object ClaudeAddressActor {
  final case class WhatIsClaudeAddress()
  trait Factory {
    def apply(executionContext: ExecutionContext, wSClient: WSClient): Actor
  }
}

class ClaudeAddressActor @Inject()(implicit val ec: ExecutionContext,
                                   wSClient: WSClient) extends Actor with ActorsLoggerHelper with ActorLogging {

  val claudeBaseUrl = "http://localhost:9000"
//  val claudeBaseUrl = "https://claude.wtf"

  override def receive = {
    case _: WhatIsClaudeAddress =>
      val senderCopy = sender
      wSClient
        .url(claudeBaseUrl + "/whatMyAddress")
        .withRequestTimeout(10.seconds)
        .get()
        .map { address =>
          senderCopy ! address.body
          self ! PoisonPill
        }
        .recover { case NonFatal(e) => logE(e) }

    case _ =>
      log error "Unhandled message"
  }
}
