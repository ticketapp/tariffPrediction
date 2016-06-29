package organizersDomain

import javax.inject.Inject

import APIs.{FacebookAPI, FormatResponses}
import APIs.FacebookAPI.FacebookOrganizer
import addresses.{Address, GeographicPointTrait, SortableByGeographicPoint}
import com.vividsolutions.jts.geom.{Coordinate, Geometry, GeometryFactory}
import logger.LoggerHelper

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps
import json.JsonHelper._

final case class Organizer(id: Option[Long] = None,
                           facebookId: String,
                           facebookUrl: String,
                           name: String,
                           description: Option[String] = None,
                           addressId: Option[Long] = None,
                           phone: Option[String] = None,
                           publicTransit: Option[String] = None,
                           websites: Option[String] = None,
                           verified: Boolean = false,
                           imagePath: Option[String] = None,
                           var geographicPoint: Geometry = new GeometryFactory().createPoint(new Coordinate(-84, 30)),
                           linkedPlaceUrl: Option[String] = None,
                           likes: Option[Long] = None)

@SerialVersionUID(42L)
final case class OrganizerWithAddress(organizer: Organizer,
                                      maybeAddress: Option[Address] = None)
    extends SortableByGeographicPoint with GeographicPointTrait{
  private def returnEventGeographicPointInRelations(organizer: Organizer,
                                                    maybeAddress: Option[Address]): Geometry =
    organizer.geographicPoint match {
      case notAntarcticPoint if notAntarcticPoint != antarcticPoint =>
        notAntarcticPoint

      case _ =>
        val addressesGeoPoints = maybeAddress map(_.geographicPoint)
        val organizerGeoPoint = Option(organizer.geographicPoint)
        val geoPoints = Seq(addressesGeoPoints, organizerGeoPoint).flatten

        geoPoints find(_ != antarcticPoint) match {
          case Some(geoPoint) => geoPoint
          case _ => antarcticPoint
        }
    }

  val geographicPoint: Geometry = returnEventGeographicPointInRelations(organizer, maybeAddress)
  this.organizer.geographicPoint = geographicPoint
}

class OrganizerMethods @Inject()(facebookAPI: FacebookAPI) extends LoggerHelper with FormatResponses {

  def getOrganizerOnFacebook(organizerId: String): Future[OrganizerWithAddress] = {
    facebookAPI.getOrganizer(organizerId) map { organizer =>
      facebookOrganizerToOrganizer(fromJsValueTo[FacebookOrganizer](organizer))
    }
  }

  private def facebookOrganizerToOrganizer(facebookOrganizer: FacebookOrganizer): OrganizerWithAddress = {
    val address = extractAddressFromLocation(facebookOrganizer.location)
    val geographicPoint = extractGeographicPointFromLocation(facebookOrganizer.location)
      .getOrElse(new GeometryFactory().createPoint(new Coordinate(-84, 30)))

    val organizer = Organizer(
      facebookId = facebookOrganizer.id,
      name = facebookOrganizer.name,
      facebookUrl = refactorFacebookLink(facebookOrganizer.link),
      description = facebookOrganizer.description,
      phone = facebookOrganizer.phone,
      publicTransit = facebookOrganizer.public_transit,
      websites = facebookOrganizer.website,
      imagePath = extractImagePath(facebookOrganizer.cover),
      geographicPoint = geographicPoint,
      likes = Option(facebookOrganizer.fan_count))

    OrganizerWithAddress(organizer, address)
  }
}

class OrganizerMethodsMock @Inject()(facebookAPI: FacebookAPI) extends OrganizerMethods(facebookAPI: FacebookAPI) {
  override def getOrganizerOnFacebook(organizerId: String): Future[OrganizerWithAddress] =
    Future.successful(OrganizerWithAddress(Organizer(
      name = "name",
      facebookId = "facebookId",
      facebookUrl = "facebookUrl")))
}
