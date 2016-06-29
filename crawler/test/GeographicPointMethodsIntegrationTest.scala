import addresses.{Address, GeographicPointTrait}
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}
import testsHelper.testHelper

import scala.language.postfixOps
import testsHelper.Injectors.searchGeographicPoint

class GeographicPointMethodsIntegrationTest extends testHelper with GeographicPointTrait {

  "A geographicPoint" must {

    "get a geographicPoint" in {
      val address = Address(city = Option("privas"), zip = Option("07000"), street = Option("avignas"))
      val geoPoint = optionStringToPoint(Option("44.735269,4.599038999999999"))
      whenReady(searchGeographicPoint.getGeographicPoint(address = address, retry = 3),
        timeout(Span(2, Seconds))) { addressWithGeoPoint =>

        addressWithGeoPoint.geographicPoint mustBe geoPoint
      }
    }
  }
}
