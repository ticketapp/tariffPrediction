package artistsDomain

import javax.inject.Inject

import json.JsonHelper._
import logger.LoggerHelper
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.mvc._
import services.Utilities
import scala.language.postfixOps

class ArtistController @Inject()(val messagesApi: MessagesApi,
                                 artistMethods: ArtistMethods)
    extends Controller with Utilities with LoggerHelper {

  def getFacebookArtistsContaining(pattern: String) = Action.async {
    artistMethods.getFacebookArtistsAndReadThem(pattern).map { artists =>
      Ok(Json.toJson(artists))
    } recover { case t: Throwable =>
      Logger.error("ArtistController.getFacebookArtistsContaining: ", t)
      InternalServerError("ArtistController.getFacebookArtistsContaining: " + t.getMessage)
    }
  }
/*
  def create = Action.async { implicit request =>
    artistWithPatternBindingForm.bindFromRequest().fold(
      formWithErrors => {
        Logger.error(formWithErrors.errorsAsJson.toString())
        Future(BadRequest(formWithErrors.errorsAsJson))
      },

      patternAndArtist => {
        artistMethods.saveOrReturnNoneIfDuplicate(ArtistWithWeightedGenres(patternAndArtist.artistWithWeightedGenres.artist,
          patternAndArtist.artistWithWeightedGenres.genres)) map {
          case Some(artist) =>
            val artistWithArtistId = patternAndArtist.artistWithWeightedGenres.artist.copy(id = artist.id)
            val patternAndArtistWithArtistId = PatternAndArtist(
              searchPattern = patternAndArtist.searchPattern,
              artistWithWeightedGenres = ArtistWithWeightedGenres(artistWithArtistId, Vector.empty))

            val tracksEnumerator = artistMethods.getArtistTracks(patternAndArtistWithArtistId)

            val jsonTracksEnumerator = tracksEnumerator &>
              trackMethods.filterDuplicateTracksEnumeratee &>
              trackMethods.saveTracksInFutureEnumeratee &>
              trackMethods.toJsonEnumeratee

            Ok.chunked(jsonTracksEnumerator.andThen(Enumerator(Json.toJson("end"))))

          case None =>
            Conflict
        }
      }
    )
  }*/
}
