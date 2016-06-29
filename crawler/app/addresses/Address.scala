package addresses

import com.vividsolutions.jts.geom.{Coordinate, GeometryFactory, Geometry}

final case class Address(id: Option[Long] = None,
                         geographicPoint: Geometry = new GeometryFactory().createPoint(new Coordinate(-84, 30)),
                         city: Option[String] = None,
                         zip: Option[String] = None,
                         street: Option[String] = None,
                         country: Option[String] = None) {
  require(
    !(geographicPoint == new GeometryFactory().createPoint(new Coordinate(-84, 30)) && city.isEmpty &&
      zip.isEmpty && street.isEmpty && country.isEmpty),
    "address must contain at least one field")
}