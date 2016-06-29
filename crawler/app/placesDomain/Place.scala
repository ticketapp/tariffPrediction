package placesDomain

import addresses.{Address, SortableByGeographicPoint}
import com.vividsolutions.jts.geom.{Coordinate, Geometry, GeometryFactory}
import org.joda.time.DateTime
import services._

import scala.language.postfixOps

final case class Place(id: Option[Long] = None,
                       name: String,
                       facebookId: String,
                       facebookUrl: String,
                       var geographicPoint: Geometry = new GeometryFactory().createPoint(new Coordinate(-84, 30)),
                       description: Option[String] = None,
                       websites: Option[String] = None,
                       capacity: Option[Int] = None,
                       openingHours: Option[String] = None,
                       imagePath: Option[String] = None,
                       addressId: Option[Long] = None,
                       linkedOrganizerUrl: Option[String] = None,
                       likes: Option[Long] = None)

@SerialVersionUID(42L)
case class PlaceWithAddress(place: Place, maybeAddress: Option[Address] = None)
    extends SortableByGeographicPoint with Utilities {

  private def returnPlaceGeographicPointInRelations(place: Place, maybeAddress: Option[Address]): Geometry = {
    place.geographicPoint match {
      case notAntarcticPoint if notAntarcticPoint != antarcticPoint =>
        notAntarcticPoint

      case _ =>
        maybeAddress map(_.geographicPoint) match {
          case Some(notAntarcticPoint) if notAntarcticPoint != antarcticPoint => notAntarcticPoint
          case _ => antarcticPoint
        }
    }
  }

  val geographicPoint: Geometry = returnPlaceGeographicPointInRelations(place, maybeAddress)
  this.place.geographicPoint =  geographicPoint
}

@SerialVersionUID(42L)
final case class UpdatePlace(place: PlaceWithAddress)

@SerialVersionUID(42L)
final case class GetPlace(offset: Long, notUpdatedSince: DateTime)
