package APIs

import javax.inject.Inject

import play.api.libs.json._
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}

object FacebookAPI {
  final case class Cover(offset_x: Int, offset_y: Int, source: String)
  final case class Owner(name: String, id: String)
  final case class Location(street: Option[String],
                            zip: Option[String],
                            city: Option[String],
                            country: Option[String],
                            latitude: Option[Double],
                            longitude: Option[Double])
  final case class FacebookAttendee(name: String, id: String, rsvp_status: String)
  final case class FacebookPlace(id: Option[String],
                                 about: Option[String],
                                 location: Option[Location],
                                 website: Option[String],
                                 cover: Option[Cover],
                                 name: String,
                                 fan_count: Option[Long],
                                 link: Option[String])
  final case class FacebookEvent(cover: Option[Cover],
                                 description: Option[String],
                                 name: String,
                                 start_time: String,
                                 end_time: Option[String],
                                 owner: Owner,
                                 id: String,
                                 place: Option[FacebookPlace],
                                 location: Option[Location],
                                 attending_count: Long,
                                 declined_count : Long,
                                 interested_count: Long,
                                 maybe_count: Long,
                                 noreply_count: Long)
  final case class FacebookArtist(name: String,
                                  category: String,
                                  id: String,
                                  cover: Option[Cover],
                                  website: Option[String],
                                  link: String,
                                  description: Option[String],
                                  genre: Option[String],
                                  location: Option[Location],
                                  fan_count: Long)
  final case class FacebookOrganizer(id: String,
                                     name: String,
                                     description: Option[String],
                                     cover: Option[Cover],
                                     location: Option[Location],
                                     phone: Option[String],
                                     public_transit: Option[String],
                                     website: Option[String],
                                     fan_count: Long,
                                     link: String)

  implicit val coverFormat = Json.format[Cover]
  implicit val ownerFormat = Json.format[Owner]
  implicit val locationFormat = Json.format[Location]
  implicit val facebookPlaceFormat = Json.format[FacebookPlace]
  implicit val facebookEventWithPlaceFormat = Json.format[FacebookEvent]
  implicit val facebookArtistFormat = Json.format[FacebookArtist]
  implicit val facebookAttendeeFormat = Json.format[FacebookAttendee]
  implicit val facebookOrganizerFormat = Json.format[FacebookOrganizer]
}

class FacebookAPI @Inject() (wSClient: WSClient, implicit val ec: ExecutionContext) {
  private val token = "1434764716814175|087b04548922a278a697b4dbb709dc56"
  private val apiVersion = "v2.6"
  private val baseUrl = "https://graph.facebook.com/" + apiVersion + "/"
  private val artistFields =
    "name,cover{source,offset_x,offset_y},id,category,link,website,description,genre,location,fan_count"
  private val eventCountFields = "attending_count,declined_count,interested_count,maybe_count,noreply_count"
  private val eventFields = "cover,description,name,start_time,end_time,owner,place," + eventCountFields
  private val organizerFields =
    "name,description,cover{source,offset_x,offset_y},location,phone,public_transit,website,fan_count,link"
  private val placeFields = "about,location,website,cover,name,fan_count,link"
  private val userPagesFields = "id,name,likes{id,name,category}"

  def getArtist(normalizedFacebookUrl: String): Future[JsValue] = wSClient
    .url(baseUrl + normalizedFacebookUrl)
    .withQueryString(
      "fields" -> artistFields,
      "access_token" -> token)
    .get()
    .map(_.json)

  def getUserPages(userAccessToken: String): Future[JsValue] = wSClient
    .url(baseUrl + "me")
    .withQueryString(
      "fields" -> userPagesFields,
      "access_token" -> userAccessToken)
    .get()
    .map(_.json)

  def getArtists(pattern: String): Future[JsValue] = {
    if (pattern.length >= 2)
      wSClient
        .url(baseUrl + "search")
        .withQueryString(
          "q" -> pattern,
          "type" -> "page",
          "limit" -> "400",
          "fields" -> artistFields,
          "access_token" -> token)
        .get()
        .map(_.json)
    else
      Future.successful(Json.parse("""{"data": []"""))
  }

  def getEvent(eventFacebookId: String): Future[JsValue] = wSClient
    .url(baseUrl + eventFacebookId)
    .withQueryString(
      "fields" -> eventFields,
      "access_token" -> token)
    .get()
    .map(_.json)

  def getEventsId(facebookId: String): Future[JsValue] = wSClient
    .url(baseUrl + facebookId + "/events/")
    .withQueryString("access_token" -> token)
    .get()
    .map(_.json)

  def getAttendees(eventFacebookId: String): Future[JsValue] = wSClient
    .url(baseUrl + eventFacebookId + "/attending")
    .withQueryString(
      "access_token" -> token,
      "limit" -> "400")
    .get()
    .map(_.json)

  def getOrganizer(organizerId: String): Future[JsValue] = wSClient
    .url(baseUrl + organizerId)
    .withQueryString(
    "fields" -> organizerFields,
    "access_token" -> token)
    .get()
    .map(_.json)

  def getPlace(placeFacebookId: String): Future[JsValue] = wSClient
    .url(baseUrl + placeFacebookId)
    .withQueryString(
      "fields" -> placeFields,
      "access_token" -> token)
    .get()
    .map(_.json)
}

class FacebookAPIMock @Inject() (wSClient: WSClient, ec: ExecutionContext)
    extends FacebookAPI(wSClient: WSClient, ec: ExecutionContext) {

  override def getOrganizer(organizerId: String): Future[JsValue] = {
    val facebookResponse = Json.parse(
      """{"name":"Le Transbordeur","description":"Ancienne usine destinée à l’origine au traitement des eaux, le bâtiment situé sur la commune de Villeurbanne, au 3 boulevard de la bataille de Stalingrad, est depuis plus de 25 ans voué à accueillir concerts, spectacles, galas et événements culturels.\nLa belle histoire démarre le 21 janvier 1989, avec la venue du groupe britannique New Order.\nDepuis, difficile de dresser une liste de tous les artistes qui ont un jour posé leurs amplis sur la scène du Transbordeur, presque tous les grands noms du rock international, de la chanson, du hip-hop, des musiques électroniques, y ont joué.\nSa programmation éclectique, qui fait perdurer rock, en fait un lieu de référence au niveau régional, national, voire international !","cover":{"source":"https://scontent.xx.fbcdn.net/v/t1.0-9/s720x720/13432392_1056819411020685_4634784103209873694_n.png?oh=a6c4f3b2f2e2d5657f70b4377b9b0f4c&oe=57CAF626","offset_x":0,"offset_y":0,"id":"1056819411020685"},"location":{"city":"Villeurbanne","country":"France","latitude":45.783879926337,"longitude":4.8606577145685,"street":"3 boulevard de la bataille de Stalingrad","zip":"69100"},"phone":"04 78 93 08 33","public_transit":"Accés Bus : Ligne C1, 58 et 4 – arrêt Cité Internationale / Ligne 59 et 70 – arrêt Cité internationale - Transbordeur\nParking gratuit / Station Velov' à proximité.","website":"www.transbordeur.fr","fan_count":22571,"id":"164354640267171", "link": "https://www.facebook.com/pages/Le-Transbordeur/164354640267171"}"""
    )
    Future.successful(facebookResponse)
  }
}
