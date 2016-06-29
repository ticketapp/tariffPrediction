package artistsDomain

import javax.inject.Inject

import akka.actor.{Actor, ActorLogging}
import akka.pattern.ask
import application.IsFinished
import artistsDomain.ArtistsSupervisor.IncrementCounter
import com.google.inject.assistedinject.Assisted
import eventsDomain.EventWithRelations
import facebookLimit.FacebookLimit
import genresDomain.Genre
import json.JsonHelper._
import logger.ActorsLoggerHelper
import play.api.libs.json.JsValue
import services.Utilities
import websites.{UnshortLinksFactory, Websites}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

object GetArtistsActor {
  trait Factory {
    def apply(ec: ExecutionContext,
              unshortLinksFactory: UnshortLinksFactory,
              artistMethods: ArtistMethods,
              claudeAddress: String): Actor
  }
}

class GetArtistsActor @Inject() (unshortLinksFactory: UnshortLinksFactory,
                                 artistMethods: ArtistMethods,
                                 websites: Websites,
                                 implicit val ec: ExecutionContext,
                                 @Assisted claudeAddress: String)
    extends Actor with ActorLogging with ActorsLoggerHelper with Utilities with FacebookLimit {
  implicit val timeout: akka.util.Timeout = 5.seconds

  val claudeEventActor = context
    .actorSelection(s"$claudeAddress/user/event-actor")
    .resolveOne()

  val claudeArtistActor = context
    .actorSelection(s"$claudeAddress/user/artist-actor")
    .resolveOne()

  startToFindArtistsForEvent(0)

  val supervisorPath = "/user/artists-and-genres-supervisor"

  def startToFindArtistsForEvent(eventOffset: Long): Unit = findArtistsForEvent(eventOffset) map {
    case IsFinished(true) =>
      context
        .actorSelection(supervisorPath)
        .resolveOne()
        .map(actorRef => actorRef ! IsFinished(true))
        .recover { case e => logE(e) }

    case IsFinished(false) =>
      context
        .actorSelection(supervisorPath)
        .resolveOne()
        .map(actorRef => actorRef ! IncrementCounter)
        .recover { case e => logE(e) }
  }

  def findArtistsForEvent(offset: Long = 0): Future[IsFinished] = claudeEventActor.flatMap { actorRef =>
    val eventuallyEventJson: Future[JsValue] = (actorRef ? offset).mapTo[JsValue]

    eventuallyEventJson flatMap { eventJson =>
      eventJson.validate[Seq[EventWithRelations]].get.headOption match {
        case Some(event) => getAndSaveArtistsAndGenres(event)
        case None => Future.successful(IsFinished(true))
      }
    }
  }.recover { case e =>
    logE(e)
    IsFinished(false)
  }

  def getAndSaveArtistsAndGenres(event: EventWithRelations): Future[IsFinished] = getArtistsAndGenres(event)
    .map { artistsAndGenres =>
    if (artistsAndGenres._1.nonEmpty) {
      claudeArtistActor map { actorRef =>
        actorRef ! EventIdArtistsAndGenres(event.event.facebookId, artistsAndGenres._1, artistsAndGenres._2)
      }
    }
    IsFinished(false)
  }.recover { case e: FacebookRequestLimit =>
    informSupervisorOfLimitRequest("get-artists-supervisor")
    IsFinished(false)
  }

  def getArtistsAndGenres(event: EventWithRelations): Future[(Seq[ArtistWithWeightedGenres], Seq[Genre])] = for {
    normalizedWebsites <- websites.getUnshortedNormalizedWebsites(event.event.description)
    artistsFromDescription <- artistMethods.getFacebookArtistsByWebsites(normalizedWebsites)
    artistsFromTitle <- artistMethods.getEventuallyArtistsInEventTitle(event.event.name, normalizedWebsites)
    nonEmptyArtists = (artistsFromDescription.toVector ++ artistsFromTitle).distinct
    artistGenres = nonEmptyArtists.flatMap(_.genres map(_.genre))
    //    overGenres <- genreMethods.findOverGenres(artistGenres)
    //    genres = (artistGenres ++ overGenres).distinct
    genres = artistGenres.distinct
  } yield (nonEmptyArtists, genres)

  override def receive: Receive = {
    case offset: Long  => startToFindArtistsForEvent(offset)
    case e: Exception  => throw e
    case _             => logE("Unhandled message")
  }
}
