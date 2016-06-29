package APIs

import javax.inject.Inject

import artistsDomain.Artist
import logger.LoggerHelper
import play.api.Logger
import play.api.libs.json.JsValue
import play.api.libs.ws.{WSClient, WSResponse}
import tracksDomain.Track

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class SoundCloudAPI @Inject() (wSClient: WSClient, implicit val ec: ExecutionContext)
    extends SoundCloudReaders with LoggerHelper {
  val baseUrl = "http://api.soundcloud.com/users/"
  val clientId = "2a37f8098705604b8679fd079d5ad90f"

  def getFacebookUrlBySoundCloudUrl(soundCloudUrl: String): Future[WSResponse] = {
    val soundCloudName = soundCloudUrl.substring(soundCloudUrl.indexOf("/") + 1)

    wSClient.url(baseUrl + soundCloudName + "/web-profiles")
      .withQueryString("client_id" -> clientId)
      .get()
  }

  def getWebsites(idOrNormalizedUrl: String): Future[JsValue] = wSClient
    .url(baseUrl + idOrNormalizedUrl + "/web-profiles")
    .withQueryString("client_id" -> clientId)
    .get()
    .map(_.json)

  def getIdsForName(namePattern: String): Future[Seq[Long]] = wSClient
    .url(baseUrl)
    .withQueryString(
      "q" -> namePattern,
      "client_id" -> clientId)
    .get()
    .map(readSoundCloudIds)
    .recover {
      case NonFatal(e) =>
        Logger.error("SearchSoundCloudTracks.getSoundCLoudIdsForName: " + namePattern, e)
        Seq.empty
    }

  def getTracksWithSoundCloudLink(soundCloudLink: String, artist: Artist): Future[Seq[Track]] = wSClient
    .url(baseUrl + soundCloudLink + "/tracks")
    .withQueryString("client_id" -> clientId)
    .get()
    .map{ response =>
      readSoundCloudTracks(response.json, artist)
    } recover {
    case NonFatal(e) =>
      log("SearchSoundcloudTracks.getSoundCloudTracksWithSoundCloudLink: for: " + soundCloudLink + "\nMessage:\n", e)
      Seq.empty
  }
}
