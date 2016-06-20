import org.scalatest.concurrent.ScalaFutures._
import testsHelper.testHelper

class EventTest extends testHelper {
  "An event" must {

    "be found" in {
      whenReady(eventsDAO.find("182759945204685")) { event =>
        event.get.event.facebookId mustBe Some("182759945204685")
      }
    }
  }
}
