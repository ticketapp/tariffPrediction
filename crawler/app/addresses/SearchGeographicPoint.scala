package addresses

import javax.inject.Inject

import logger.LoggerHelper
import play.api.Logger
import play.api.libs.ws.WSClient
import services.Utilities

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

case class OverQueryLimit(message: String) extends Exception(message) {
  Logger.error("Over Query Limit: " + message)
}

class SearchGeographicPoint @Inject()(ws: WSClient)
    extends GeographicPointTrait
    with Utilities
    with LoggerHelper {

  def getGeographicPoint(address: Address, retry: Int): Future[Address] = ws
    .url("https://maps.googleapis.com/maps/api/geocode/json")
    .withQueryString(
      "address" -> (address.street.getOrElse("") + " " + address.zip.getOrElse("") + " " + address.city.getOrElse("")),
      "key" -> googleKey)
    .get()
    .flatMap {
      readGoogleGeographicPoint(_) match {
        case Success(Some(geographicPoint)) =>
          Future(address.copy(geographicPoint = geographicPoint))

        case Failure(e: Exception) =>
          log(e)
          Future(address)

        case _ =>
          Future(address)
      }
    } recoverWith {
    case e: OverQueryLimit if retry > 0 =>
      log("Address.getGeographicPoint: retry: " + retry + " ", e)
      getGeographicPoint(address, retry - 1)

    case NonFatal(e) =>
      log(e)
      Future(address)
  }
}
