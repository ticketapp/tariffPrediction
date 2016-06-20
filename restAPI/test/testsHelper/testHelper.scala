package testsHelper

import models.EventsDAO
import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.play.PlaySpec
import play.api.Mode
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.guice.GuiceApplicationBuilder

import scala.language.postfixOps

trait testHelper extends PlaySpec with BeforeAndAfterAll {

  lazy val appBuilder = new GuiceApplicationBuilder().in(Mode.Test)
  lazy val injector = appBuilder.injector()
  lazy val injectorWithoutActors = appBuilder.injector
  lazy val ec = scala.concurrent.ExecutionContext.Implicits.global
  lazy val dbConfProvider = injector.instanceOf[DatabaseConfigProvider]
  lazy val eventsDAO = new EventsDAO(dbConfProvider)
}
