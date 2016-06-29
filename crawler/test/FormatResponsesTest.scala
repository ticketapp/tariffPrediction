import APIs.FormatResponses
import testsHelper.testHelper

class FormatResponsesTest extends testHelper with FormatResponses {

  "A facebook link" must {

    "reformated" in {
      refactorFacebookLink("https://www.facebook.com/Sucre.lyon/") mustBe "Sucre.lyon"
    }
  }
}
