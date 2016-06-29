import application.ExtractLabeledPoint
import org.scalatestplus.play.PlaySpec

class ExtractLabeledPointTest extends PlaySpec with ExtractLabeledPoint {

  "The max price" must {

    "be extracted" in {
      extractMaxTariff("0-0") mustBe 0
      extractMaxTariff("7-15") mustBe 15
    }
  }
}
