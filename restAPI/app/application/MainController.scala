package application

import javax.inject.Inject

import models.{EventsDAO, jsonImplicits}
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}

import scala.concurrent.ExecutionContext

class MainController @Inject() (eventsDAO: EventsDAO, implicit val ec: ExecutionContext)
    extends Controller with jsonImplicits {

  def findEvent(facebookId: String) = Action.async {
    eventsDAO.find(facebookId: String) map(e => Ok(Json.toJson(e)))
  }

  def predictTariff = Action {
    Ok(Json.toJson(0))
  }
}
