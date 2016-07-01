package daoActors

import javax.inject.Inject

import akka.actor.{Actor, ActorLogging}
import logger.ActorsLoggerHelper
import models.{GetPlace, PlaceWithAddress, UpdatePlace}
import modelsMethods.PlaceMethods

import scala.concurrent.ExecutionContext

class PlaceActor @Inject()(placeMethods: PlaceMethods,
                           implicit val ec: ExecutionContext) extends Actor with ActorsLoggerHelper with ActorLogging {

  def sendPlace(placeOffset: Long): Unit = {
    val senderCopy = sender
    placeMethods.findSinceOffset(offset = placeOffset, numberToReturn = 1) map(senderCopy ! _)
  }

  def sendPlace(getPlaceNotUpdatedSince: GetPlace): Unit = {
    val senderCopy = sender
    placeMethods
      .findSinceOffset(
        offset = getPlaceNotUpdatedSince.offset,
        numberToReturn = 1,
        notUpdatedSince = getPlaceNotUpdatedSince.notUpdatedSince)
      .map(senderCopy ! _)
  }

  override def receive: Receive = {
    case place: PlaceWithAddress            => placeMethods.save(place.place)
    case placeOffset: Long                  => sendPlace(placeOffset)
    case getPlaceNotUpdatedSince: GetPlace  => sendPlace(getPlaceNotUpdatedSince)
    case updatePlace: UpdatePlace           => placeMethods.update(updatePlace.place.place)
    case _                                  => log error "Unhandled message"
  }
}
