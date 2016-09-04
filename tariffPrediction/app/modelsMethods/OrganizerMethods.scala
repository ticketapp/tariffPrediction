package modelsMethods

import javax.inject.Inject

import database.MyPostgresDriver.api._
import database.{EventOrganizerRelation, MyDBTableDefinitions, MyPostgresDriver}
import logger.LoggerHelper
import models.{Organizer, OrganizerWithAddress}
import play.api.Logger
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.control.NonFatal

class OrganizerMethods @Inject()(protected val dbConfigProvider: DatabaseConfigProvider,
                                 placeMethods: PlaceMethods)
    extends HasDatabaseConfigProvider[MyPostgresDriver]
    with MyDBTableDefinitions
    with LoggerHelper {

  def findSinceOffset(offset: Long, numberToReturn: Long): Future[Seq[OrganizerWithAddress]] = {
    val tupledJoin = organizers.drop(offset).take(numberToReturn) joinLeft addresses on (_.addressId === _.id)

    db.run(tupledJoin.result) map(_ map OrganizerWithAddress.tupled)
  }

  def findById(id: Long): Future[Option[OrganizerWithAddress]] = {
    val query = organizers.filter(_.id === id) joinLeft addresses on (_.addressId === _.id)
    db.run(query.result.headOption).map {
      case Some(organizer) => Option(OrganizerWithAddress.tupled(organizer))
      case None => None
    }
  }

  def findByFacebookUrl(url: String): Future[Option[OrganizerWithAddress]] = {
    val query = organizers.filter(_.facebookUrl === url) joinLeft addresses on (_.addressId === _.id)
    db.run(query.result.headOption).map {
      case Some(organizer) => Option(OrganizerWithAddress.tupled(organizer))
      case None => None
    }
  }

  def findByFacebookId(facebookId: String): Future[Option[OrganizerWithAddress]] = {
    val query = organizers.filter(_.facebookId === facebookId) joinLeft addresses on (_.addressId === _.id)
    db.run(query.result.headOption).map {
      case Some(organizer) => Option(OrganizerWithAddress.tupled(organizer))
      case None => None
    }
  }

  def findAllContaining(pattern: String): Future[Seq[OrganizerWithAddress]] = {
    val lowercasePattern = pattern.toLowerCase
    val query = for {
      organizerWithAddress <- organizers joinLeft addresses on (_.addressId === _.id)
      if organizerWithAddress._1.name.toLowerCase like s"%$lowercasePattern%"
    } yield organizerWithAddress
    db.run(query.result) map(_ map OrganizerWithAddress.tupled)
  }

  def save(organizer: Organizer): Future[Organizer] = {
    placeMethods.findUrlByFacebookId(organizer.facebookId) flatMap { maybePlaceUrl =>
      val organizerToSave = organizer.copy(
        phone = organizer.phone,
        linkedPlaceUrl = maybePlaceUrl)

      doSave(organizerToSave)
    }
  }

  def doSave(organizer: Organizer): Future[Organizer] = {
    val query = for {
      organizerFound <- organizers.filter(_.facebookId === organizer.facebookId).result.headOption
      result <- organizerFound.map(DBIO.successful).getOrElse(organizers returning organizers.map(_.id) += organizer)
    } yield result

    db.run(query) map {
      case organizer: Organizer => organizer
      case id: Long => organizer.copy(id = Option(id))
    }
  }

  def saveWithEventRelation(organizer: Organizer, eventId: String): Future[Organizer] =
    save(organizer) flatMap { savedOrganizer =>
      saveEventRelation(EventOrganizerRelation(eventId, savedOrganizer.facebookUrl)) map {
        case 1 =>
          savedOrganizer

        case _ =>
          Logger.error("Organizer.saveWithEventRelation: not exactly one row was updated by saveEventRelation")
          savedOrganizer
      }
    }

  def saveEventRelation(eventOrganizerRelation: EventOrganizerRelation): Future[Int] = {
    db.run(eventsOrganizers += eventOrganizerRelation) recover { case NonFatal(e) =>
      log(e.getMessage)
      0
    }
  }

  def saveEventRelations(eventOrganizerRelations: Seq[EventOrganizerRelation]): Future[Boolean] =
    db.run(eventsOrganizers ++= eventOrganizerRelations) map { _ =>
      true
    } recover {
      case e: Exception =>
        Logger.error("Organizer.saveEventRelations: ", e)
        false
    }
}
