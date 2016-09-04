import org.scalatest.concurrent.ScalaFutures._

class EventTest extends testHelper {
  "An event" must {

    "be found" in {
      whenReady(eventsDAO.find("212102035821858")) { event =>
        event.get.event.facebookId mustBe "212102035821858"
      }
    }
  }
}
