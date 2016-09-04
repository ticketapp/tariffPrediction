package daoActors

import javax.inject.Inject

import akka.actor.{Actor, ActorLogging}
import json.JsonImplicits
import logger.ActorsLoggerHelper
import models.EventIdArtistsAndGenres
import modelsMethods.ArtistMethods

import scala.concurrent.ExecutionContext

class ArtistActor @Inject()(implicit val executionContext: ExecutionContext,
                            artistMethods: ArtistMethods)
    extends Actor with ActorsLoggerHelper with ActorLogging with JsonImplicits {

  def saveArtist(eventIdArtistsAndGenres: EventIdArtistsAndGenres): Unit = {
    val eventId = eventIdArtistsAndGenres.eventId

    eventIdArtistsAndGenres.artistsWithWeightedGenres map { artist =>
      artistMethods.saveWithEventRelation(artist.artist, eventId)
    }
  }

  override def receive: Receive = {
    case eventIdArtistsAndGenres: EventIdArtistsAndGenres => saveArtist(eventIdArtistsAndGenres)
    case _                                                => log error "Unhandled message"
  }
}
