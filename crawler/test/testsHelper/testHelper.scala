package testsHelper

import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.play.PlaySpec

import scala.concurrent.duration._
import scala.language.postfixOps

trait testHelper extends PlaySpec with BeforeAndAfterAll {

  implicit val actorTimeout: akka.util.Timeout = 5.seconds
}
