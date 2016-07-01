package placesDomain

import javax.inject.Inject

import APIs.FacebookAPI.FacebookPlace
import APIs.{FacebookAPI, FormatResponses}
import com.vividsolutions.jts.geom.Geometry
import json.JsonHelper._
import logger.LoggerHelper
import models.{Address, Place, PlaceWithAddress}
import services._
import websites.Websites._

import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

class PlaceMethods @Inject() (facebookAPI: FacebookAPI, implicit val ec: ExecutionContext)
    extends Utilities with LoggerHelper with FormatResponses {

  def getPlace(facebookId: String): Future[PlaceWithAddress] = facebookAPI.getPlace(facebookId) map { place =>
    val facebookPlace = fromJsValueTo[FacebookPlace](place)
    facebookPlaceToPlaceWithAddress(facebookPlace).get
  }

  def facebookPlaceToPlaceWithAddress(facebookPlace: FacebookPlace): Option[PlaceWithAddress] = {
    val address = extractAddressFromLocation(facebookPlace.location)
    val geographicPoint = extractGeographicPointFromLocation(facebookPlace.location).getOrElse(antarcticPoint)

    facebookPlace.id match {
      case Some(place) => Option(createPlace(facebookPlace, address, geographicPoint))
      case _ => None
    }
  }

  def createPlace(facebookPlace: FacebookPlace,
                  address: Option[Address],
                  geographicPoint: Geometry): PlaceWithAddress = {
    PlaceWithAddress(
      place = Place(
        name = facebookPlace.name,
        facebookId = facebookPlace.id.get,
        facebookUrl = facebookPlace.link.getOrElse(facebookPlace.id.get),
        description = facebookPlace.about,
        websites = normalizeUrl(facebookPlace.website),
        capacity = None,
        openingHours = None,
        imagePath = extractImagePath(facebookPlace.cover),
        likes = facebookPlace.fan_count),
      maybeAddress = address)
  }

  def facebookPlaceToPlaceWithAddress(maybeFacebookPlace: Option[FacebookPlace]): Option[PlaceWithAddress] = {
    maybeFacebookPlace match {
      case Some(place) => facebookPlaceToPlaceWithAddress(place)
      case None => None
    }
  }
}

class PlaceMethodsMock @Inject() (facebookAPI: FacebookAPI, ec: ExecutionContext)
  extends PlaceMethods(facebookAPI: FacebookAPI, ec: ExecutionContext) {

  override def getPlace(facebookId: String): Future[PlaceWithAddress] =
    Future.successful(PlaceWithAddress(Place(name = "name", facebookId = "facebookId", facebookUrl = "Le-Transbordeur")))
}
