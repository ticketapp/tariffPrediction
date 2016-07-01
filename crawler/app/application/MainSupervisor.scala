package application

import java.util.UUID
import javax.inject.{Inject, Singleton}

import akka.actor.{Props, _}
import akka.pattern.ask
import application.ClaudeAddressActor.WhatIsClaudeAddress
import artistsDomain.{ArtistMethods, ArtistsSupervisor, GetArtistsActor}
import eventsDomain.EventMethods
import logger.ActorsLoggerHelper
import organizersDomain.{EventsByOrganizersSupervisor, GetEventsByOrganizersActor, OrganizerMethods}
import placesDomain._
import play.api.Logger
import play.api.libs.concurrent.InjectedActorSupport
import play.api.libs.ws.WSClient
import websites.UnshortLinksFactory

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

object MainSupervisor {
  abstract class HasOffset { val offset: Long }

  final case class EventsByPlacesFinishedAt(offset: Long) extends HasOffset
  final case class EventsByOrganizersFinishedAt(offset: Long) extends HasOffset
  final case class ArtistsAndGenresFinishedAt(offset: Long) extends HasOffset
  final case class UpdatePlacesFinishedAt(offset: Long) extends HasOffset

  final case class Stage(offset: Long,
                         hasBeenFinishedOnce: Boolean = false,
                         hasBeenFinishedTwice: Boolean = false)

  final case class IsFinished(boolean: Boolean)
}

@Singleton
class MainSupervisor @Inject()(getEventsByPlacesActorFactory: GetEventsByPlacesActor.Factory,
                               getEventsByOrganizersActor: GetEventsByOrganizersActor.Factory,
                               eventsByPlacesSupervisor: EventsByPlacesSupervisor.Factory,
                               eventsByOrganizersSupervisor: EventsByOrganizersSupervisor.Factory,
                               eventMethods: EventMethods,
                               placeMethods: PlaceMethods,
                               organizerMethods: OrganizerMethods,
                               artistMethods: ArtistMethods,
                               artistsAndGenresSupervisor: ArtistsSupervisor.Factory,
                               artistsAndGenresActor: GetArtistsActor.Factory,
                               unshortLinksFactory: UnshortLinksFactory,
                               claudeAddressActorFactory: ClaudeAddressActor.Factory,
                               updatePlacesSupervisorFactory: UpdatePlacesSupervisor.Factory,
                               updatePlacesActorFactory: UpdatePlacesActor.Factory,
                               wSClient: WSClient,
                               implicit val ec: ExecutionContext)
    extends Actor with InjectedActorSupport with ActorLogging with ActorsLoggerHelper {
  import MainSupervisor._
  implicit val timeout: akka.util.Timeout = 5.seconds

  val claudeAddressActor = injectedChild(claudeAddressActorFactory(ec, wSClient), UUID.randomUUID().toString)
  val claudeAddress = Await.result(claudeAddressActor ? WhatIsClaudeAddress() map(_.asInstanceOf[String]), 10.seconds)

  var eventsByPlacesStage = Stage(offset = 0)
  var eventsByOrganizersStage = Stage(offset = 0)
  var artistsStage = Stage(offset = 0)
  var placesUpdateStage = Stage(offset = 0)

  getEventsByPlaces()

  def getEventsByPlaces(): Unit = {
    Logger.info("Start events by places scheduler")
    context.become(eventsByPlaces)

    context
      .system
      .actorOf(
        Props(eventsByPlacesSupervisor(
          getEventsByPlacesActorFactory,
          eventMethods = eventMethods,
          placeMethods = placeMethods,
          wSClient = wSClient,
          claudeAddress = claudeAddress,
          startingPlaceOffset = eventsByPlacesStage.offset,
          ec = ec)),
        "events-by-places-supervisor")
  }

  def getEventsByOrganizers(): Unit = {
    Logger.info("Start events by organizers scheduler")
    context.become(eventsByOrganizers)
    context
      .system
      .actorOf(
        Props(eventsByOrganizersSupervisor(
          getEventsByOrganizersFactory = getEventsByOrganizersActor,
          organizerMethods = organizerMethods,
          eventMethods = eventMethods,
          claudeAddress = claudeAddress,
          startingOrganizerOffset = eventsByOrganizersStage.offset,
          ec = ec)),
        "events-by-organizers-supervisor")
  }

  def getArtistsByEvents(): Unit = {
    Logger.info("Start artists by events scheduler")
    context.become(artists)
    context
      .system
      .actorOf(
        Props(artistsAndGenresSupervisor(
          ec = ec,
          unshortLinksFactory = unshortLinksFactory,
          artistMethods = artistMethods,
          claudeAddress = claudeAddress,
          startingEventOffset = artistsStage.offset)),
        "artists-and-genres-supervisor")
  }

  def startUpdateOfPlaces(): Unit = {
    Logger.info("Start update places scheduler")
    context.become(placesUpdate)
    context
      .system
      .actorOf(
        Props(updatePlacesSupervisorFactory(
          updatePlacesActorFactory,
          placeMethods = placeMethods,
          wSClient = wSClient,
          claudeAddress = claudeAddress,
          startingPlaceOffset = placesUpdateStage.offset,
          ec = ec)),
        "update-places-main-supervisor")
  }

  def setNewStage(hasFinishedAt: HasOffset, lastStage: Stage): Stage = {
    if (lastStage.offset == 0 || lastStage.offset == hasFinishedAt.offset - 1) {
      val hasBeenFinishedTwice = if (lastStage.hasBeenFinishedOnce) true else false
      Stage(
        hasBeenFinishedOnce = true,
        hasBeenFinishedTwice = hasBeenFinishedTwice,
        offset = hasFinishedAt.offset)
    }
    else {
      Stage(
        hasBeenFinishedOnce = false,
        hasBeenFinishedTwice = false,
        offset = hasFinishedAt.offset)
    }
  }

  def areAllJobsDone: Boolean =  {
   val stages = Seq(eventsByPlacesStage, eventsByOrganizersStage, artistsStage, placesUpdateStage)

    if (stages.exists(_.hasBeenFinishedTwice == false)) {
      false
    }
    else {
      log info "All jobs have been done :)"
      true
    }
  }

  def placesUpdate: Receive = {
    case updatePlacesFinishedAt: UpdatePlacesFinishedAt =>
      placesUpdateStage = setNewStage(updatePlacesFinishedAt, placesUpdateStage)
      if (!areAllJobsDone) getEventsByPlaces()

    case _ =>
      logE("Unhandled message")
  }

  def artists: Receive = {
    case artistsAndGenresFinishedAt: ArtistsAndGenresFinishedAt =>
      artistsStage = setNewStage(artistsAndGenresFinishedAt, artistsStage)
      if (!areAllJobsDone) getEventsByOrganizers()

    case _ =>
      logE("Unhandled message")
  }

  def eventsByOrganizers: Receive = {
    case eventsByOrganizersFinishedAt: EventsByOrganizersFinishedAt =>
      eventsByOrganizersStage = setNewStage(eventsByOrganizersFinishedAt, eventsByOrganizersStage)
      if (!areAllJobsDone) startUpdateOfPlaces()

    case _ =>
      logE("Unhandled message")
  }

  def eventsByPlaces: Receive = {
    case eventsByPlaceFinishedAt: EventsByPlacesFinishedAt =>
      eventsByPlacesStage = setNewStage(eventsByPlaceFinishedAt, eventsByPlacesStage)
      if (!areAllJobsDone) getArtistsByEvents()

    case _ =>
      logE("Unhandled message")
  }

  override def receive: Receive = {
    case _ => logE("Unhandled message")
  }
}
