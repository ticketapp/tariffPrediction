package daoActors

import javax.inject.Inject

import akka.actor.{Actor, ActorLogging}
import database.EventPlaceRelation
import json.JsonImplicits
import models.{EventAndPlaceFacebookUrl, EventWithRelations}
import modelsMethods.{EventsMethods, PlaceMethods}
import play.api.libs.json._

import scala.concurrent.{ExecutionContext, Future}

class EventActor @Inject()(eventMethods: EventsMethods,
                           placeMethods: PlaceMethods,
                           implicit val ex: ExecutionContext) extends Actor with ActorLogging with JsonImplicits {

  def findEvent(offset: Long): Future[Seq[EventWithRelations]] =
    eventMethods.findSinceOffset(offset = offset, numberToReturn = 1)

  def saveEventWithPlaceRelation(eventWithPlaceUrl: EventAndPlaceFacebookUrl): Unit = {
    eventMethods.save(eventWithPlaceUrl.event) map { event =>
      placeMethods.saveEventRelation(EventPlaceRelation(
        eventId = event.facebookId,
        placeFacebookUrl = eventWithPlaceUrl.placeFacebookUrl))
    }
  }

  override def receive: Receive = {
    case event: EventWithRelations =>
      eventMethods.save(event)

    case eventWithPlaceId: EventAndPlaceFacebookUrl =>
      saveEventWithPlaceRelation(eventWithPlaceId)

    case offset: Long =>
      val senderCopy = sender
      findEvent(offset: Long) map(senderCopy ! Json.toJson(_))

    case _ =>
      log error "Unhandled message"
  }
}
