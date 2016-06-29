package addresses

import com.vividsolutions.jts.geom.{Coordinate, Geometry, GeometryFactory}
import play.api.Logger
import play.api.libs.ws.WSResponse
import services.Utilities

import scala.util.{Failure, Success, Try}

trait GeographicPointTrait extends Utilities {
  def readGoogleGeographicPoint(googleGeoCodeResponse: WSResponse): Try[Option[Geometry]] = googleGeoCodeResponse.statusText match {
    case "OK" =>
      val googleGeoCodeJson = googleGeoCodeResponse.json
      val latitude = ((googleGeoCodeJson \ "results")(0) \ "geometry" \ "location" \ "lat").asOpt[Double]
      val longitude = ((googleGeoCodeJson \ "results")(0) \ "geometry" \ "location" \ "lng").asOpt[Double]
      latitude match {
        case None =>
          Success(None)
        case Some(lat) =>
          longitude match {
            case None => Success(None)
            case Some(lng) => Success(Option(latAndLngToGeographicPoint(lat, lng)))
          }
      }
    case "OVER_QUERY_LIMIT" =>
      Failure(new OverQueryLimit("GeographicPointMethods.readGoogleGeographicPoint"))
    case otherStatus =>
      Failure(new Exception(otherStatus))
  }

  val geometryFactory = new GeometryFactory()

  def stringToTryPoint(string: String): Try[Geometry] = Try {
    val latitudeAndLongitude: Array[String] = string.split(",")
    latAndLngToGeographicPoint(latitudeAndLongitude(0).toDouble, latitudeAndLongitude(1).toDouble)
  }

  def latAndLngToGeographicPoint(latitude: Double, longitude: Double): Geometry = {
    val coordinate= new Coordinate(latitude, longitude)
    geometryFactory.createPoint(coordinate)
  }

  def optionStringToPoint(maybeGeographicPoint: Option[String]): Geometry = maybeGeographicPoint match {
    case Some(geoPoint) =>
      stringToTryPoint(geoPoint) match {
        case Failure(exception) =>
          Logger.error("Utilities.optionStringToPoint: ", exception)
          antarcticPoint
        case Success(validPoint) =>
          validPoint
      }
    case _ =>
      antarcticPoint
  }
}
