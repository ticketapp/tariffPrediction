package facebookLimit

import akka.actor.ActorContext
import akka.util.Timeout
import json.JsonHelper.FacebookRequestLimit
import logger.LoggerHelper

import scala.concurrent.ExecutionContext

trait FacebookLimit extends LoggerHelper {
  implicit val context: ActorContext
  implicit val timeout: Timeout
  implicit val ec: ExecutionContext

  def informSupervisorOfLimitRequest(actorName: String): Unit = context
    .actorSelection("akka://application/user/" + actorName)
    .resolveOne()
    .map { actorRef => actorRef ! FacebookRequestLimit() }
    .recover { case e => log(e.getMessage) }
}

object FacebookLimit {
  def isFacebookLimitReached(error: Throwable): Boolean = {
    error.toString.contains("Application request limit reached")
  }
}