package modelsMethods

import javax.inject.Inject

import database.MyPostgresDriver.api._
import database.{EventPlaceRelation, MyDBTableDefinitions, MyPostgresDriver}
import logger.LoggerHelper
import models.{Place, PlaceWithAddress}
import org.joda.time.DateTime
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.control.NonFatal

class PlaceMethods @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
    extends HasDatabaseConfigProvider[MyPostgresDriver]
    with MyDBTableDefinitions
    with LoggerHelper {

  def delete(id: Long): Future[Int] = db.run(places.filter(_.id === id).delete)

  def doSave(place: Place): Future[Place] = {
    val query =  for {
      placeFound <- places.filter(_.facebookId === place.facebookId).result.headOption
      result <- placeFound.map(DBIO.successful).getOrElse(places returning places.map(_.id) += place)
    } yield result

    db.run(query) map {
      case place: Place => place
      case id: Long => place.copy(id = Option(id))
    }
  }

  def save(place: Place): Future[Place] = {
    findOrganizerUrlByFacebookId(place.facebookId) flatMap { maybeOrganizerUrl =>
      val organizerWithLinkedPlace = place.copy(linkedOrganizerUrl = maybeOrganizerUrl)
      doSave(organizerWithLinkedPlace)
    }
  }

  def update(place: Place): Future[Int] = db.run(places.filter(_.id === place.id).update(place))

  def findById(id: Long): Future[Option[PlaceWithAddress]] = {
    val tupledJoin = places.filter(_.id === id) joinLeft addresses on (_.addressId === _.id)

    db.run(tupledJoin.result.headOption) map(_ map PlaceWithAddress.tupled)
  }

  def findByFacebookUrl(facebookUrl: String): Future[Option[PlaceWithAddress]] = {
    val tupledJoin = places.filter(_.facebookUrl === facebookUrl) joinLeft addresses on (_.addressId === _.id)

    db.run(tupledJoin.result.headOption) map(_ map PlaceWithAddress.tupled)
  }

  def findSinceOffset(offset: Long, numberToReturn: Long): Future[Seq[PlaceWithAddress]] = {
    val query = places.drop(offset).take(numberToReturn) joinLeft addresses on (_.addressId === _.id)
    db.run(query.result) map(_ map PlaceWithAddress.tupled)
  }

  def findSinceOffset(offset: Long, numberToReturn: Long, notUpdatedSince: DateTime): Future[Seq[PlaceWithAddress]] = {
    val query = places
      .filter(place => place.lastUpdate < notUpdatedSince)
      .drop(offset)
      .take(numberToReturn) joinLeft addresses on (_.addressId === _.id)
    db.run(query.result) map(_ map PlaceWithAddress.tupled)
  }

  def findOrganizerUrlByFacebookId(facebookId: String): Future[Option[String]] =
    db.run(organizers.filter(_.facebookId === facebookId).map(_.facebookUrl).result.headOption)

  def findUrlByFacebookId(facebookId: String): Future[Option[String]] =
    db.run(places.filter(_.facebookId === facebookId).map(_.facebookUrl).result.headOption) recover { case NonFatal(e) =>
      log(e)
      None
    }

  def saveEventRelation(eventPlaceRelation: EventPlaceRelation): Future[Int] =
    db.run(eventsPlaces += eventPlaceRelation) recover { case NonFatal(e) =>
      log(s"The relation $eventPlaceRelation was not saved\n" + e.getMessage)
      0
    }

  def deleteEventRelation(eventPlaceRelation: EventPlaceRelation): Future[Int] = db.run(eventsPlaces
    .filter(eventPlace => eventPlace.eventId === eventPlaceRelation.eventId &&
      eventPlace.placeFacebookUrl === eventPlaceRelation.placeFacebookUrl)
    .delete)

  def saveWithEventRelation(place: PlaceWithAddress, eventId: String): Future[Place] = {
    save(place.place) flatMap { savedPlace =>
      saveEventRelation(EventPlaceRelation(eventId, savedPlace.facebookUrl)) map {
        case 1 =>
          savedPlace

        case _ =>
          log(s"Not exactly one row saved by Place.saveEventRelation for place $savedPlace and eventId $eventId")
          savedPlace
      }
    }
  }
}
