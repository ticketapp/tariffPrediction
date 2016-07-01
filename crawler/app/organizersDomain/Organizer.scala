package organizersDomain

import javax.inject.Inject

import APIs.{FacebookAPI, FormatResponses}
import APIs.FacebookAPI.FacebookOrganizer
import com.vividsolutions.jts.geom.{Coordinate, Geometry, GeometryFactory}
import logger.LoggerHelper

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps
import json.JsonHelper._
import models.{Organizer, OrganizerWithAddress}

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
