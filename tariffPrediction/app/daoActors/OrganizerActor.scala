package daoActors

import javax.inject.Inject

import akka.actor.{Actor, ActorLogging}
import json.JsonImplicits
import models.OrganizerWithAddress
import modelsMethods.OrganizerMethods
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}

import scala.concurrent.ExecutionContext

class OrganizerActor @Inject()(organizerMethods: OrganizerMethods,
                               implicit val ex: ExecutionContext) extends Actor with ActorLogging with JsonImplicits {

  def save(organizer: JsValue): Unit = organizer.validate[OrganizerWithAddress] match {
    case jsError: JsError =>
      log error jsError.toString
    case organizerWithAddress: JsSuccess[OrganizerWithAddress] =>
      organizerMethods.save(organizerWithAddress.get.organizer)
  }

  def sendOrganizer(offset: Long): Unit = {
    val senderCopy = sender
    organizerMethods.findSinceOffset(offset, 1) map (senderCopy ! Json.toJson(_))
  }

  override def receive: Receive = {
    case organizer: JsValue => save(organizer)
    case offset: Long       => sendOrganizer(offset)
    case _                  => log error "Unhandled message"
  }
}
