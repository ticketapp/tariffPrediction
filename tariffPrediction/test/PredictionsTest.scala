import org.specs2.specification.{AfterAll, BeforeAll}
import play.api.Play
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.{FakeRequest, PlaySpecification}

import scala.language.postfixOps

class PredictionsTest extends PlaySpecification with BeforeAll with AfterAll {
  lazy val fakeApplication = new GuiceApplicationBuilder().build()

  override def beforeAll(): Unit = Play.start(fakeApplication)

  override def afterAll(): Unit = Play.stop(fakeApplication)

  "A prediction" should {

    "be returned" in {
      val labels = Json.parse(
        """{
          "attendingCount": 1,
          "declinedCount": 1,
          "interestedCount": 1,
          "maybeCount": 1,
          "noreplyCount": 1,
          "organizersLikes": [1],
          "placeCapacity": 1,
          "placeLike": 1,
          "artistsLikes": [1]
        }""")
      val Some(prediction) = route(fakeApplication, FakeRequest(application.routes.MainController.predict())
        .withJsonBody(labels))
      

      contentAsString(prediction).toInt mustEqual 0
    }
  }

  "Bad request" should {

    "be returned as HTTP status" in {

      val Some(prediction) = route(fakeApplication, FakeRequest(application.routes.MainController.predict())
        .withJsonBody(Json.parse("{}")))

      status(prediction) mustEqual BAD_REQUEST
    }
  }
}
