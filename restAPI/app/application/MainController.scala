package application

import javax.inject.Inject

import models.{EventsDAO, jsonImplicits}
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.mvc.{Action, Controller}

import scala.concurrent.{ExecutionContext, Future}

final case class PredictionLabels(attendingCount: Long,
                                  declinedCount: Long,
                                  interestedCount: Long,
                                  maybeCount: Long,
                                  noreplyCount: Long,
                                  organizersLikes: Seq[Long],
                                  placeCapacity: Long,
                                  placeLike: Long,
                                  artistsLikes: Seq[Long])

class MainController @Inject() (eventsDAO: EventsDAO, implicit val ec: ExecutionContext)
    extends Controller with jsonImplicits {

  def findEvent(facebookId: String) = Action.async {
    eventsDAO.find(facebookId: String) map(e => Ok(Json.toJson(e)))
  }

  def predict = Action.async(parse.json) { request =>
    request.body.validate[PredictionLabels] match {
      case predictionLabels: JsSuccess[PredictionLabels] => Future.successful(Ok(Json.toJson(0)))
      case errors: JsError => Future.successful(BadRequest(errors.toString))
    }
  }
}
