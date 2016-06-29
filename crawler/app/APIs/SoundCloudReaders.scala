package APIs

import java.util.UUID._

import artistsDomain.Artist
import play.api.libs.json._
import play.api.libs.ws.WSResponse
import tracksDomain.{Track, TrackMethods}
import play.api.libs.functional.syntax._

trait SoundCloudReaders extends TrackMethods {
  def readSoundCloudIds(soundCloudWSResponse: WSResponse): Seq[Long] = {
    val readSoundCloudIds: Reads[Seq[Long]] = Reads.seq((__ \ "id").read[Long])
    soundCloudWSResponse.json
      .asOpt[Seq[Long]](readSoundCloudIds)
      .getOrElse(Seq.empty)
  }

  def readSoundCloudTracks(soundCloudJsonWSResponse: JsValue, artist: Artist): Seq[Track] = {
    val soundCloudTrackReads = (
      (__ \ "stream_url").readNullable[String] and
        (__ \ "title").readNullable[String] and
        (__ \ "permalink_url").readNullable[String] and
        (__ \ "user" \ "avatar_url").readNullable[String] and
        (__ \ "artwork_url").readNullable[String] and
        (__ \ "genre").readNullable[String]
      )((url: Option[String], title: Option[String], redirectUrl: Option[String], avatarUrl: Option[String],
         thumbnail: Option[String], genre: Option[String]) =>
      (url, title, redirectUrl, thumbnail, avatarUrl, genre))

    val onlyTracksWithUrlTitleAndThumbnail =
      Reads.seq(soundCloudTrackReads).map(collectOnlyValidTracks(_, artist))

    soundCloudJsonWSResponse
      .asOpt[Seq[Track]](onlyTracksWithUrlTitleAndThumbnail)
      .getOrElse(Seq.empty)
  }

  def collectOnlyValidTracks(tracks: Seq[(Option[String], Option[String], Option[String], Option[String],
    Option[String], Option[String])], artist: Artist): Seq[Track] = {
    tracks.collect {
      case (Some(url), Some(title), redirectUrl: Option[String], Some(thumbnailUrl: String), avatarUrl, genre) =>
        Track(randomUUID, normalizeTrackTitle(title, artist.name), url, 's', thumbnailUrl,
          artist.facebookUrl, artist.name, redirectUrl)

      case (Some(url), Some(title), redirectUrl: Option[String], None, Some(avatarUrl: String), genre) =>
        Track(randomUUID, normalizeTrackTitle(title, artist.name), url, 's', avatarUrl,
          artist.facebookUrl, artist.name, redirectUrl)
    }
  }
}
