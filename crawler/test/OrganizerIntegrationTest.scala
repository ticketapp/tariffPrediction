import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}
import testsHelper.testHelper
import testsHelper.Injectors.organizerMethods

class OrganizerIntegrationTest extends testHelper {

  "Info about the organizer" must {

    "be get on Facebook" in {
      whenReady(organizerMethods.getOrganizerOnFacebook("164354640267171"), timeout(Span(5, Seconds))) { organizer =>

        organizer.organizer.name mustBe "Le Transbordeur"
      }
    }
  }
}
