package application

import javax.inject.Inject

import akka.actor.{ActorSystem, PoisonPill, Props}
import application.ClusterAddresses.WhatMyAddress
import json.JsonImplicits
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.mvc.{Action, Controller}

import scala.concurrent.{ExecutionContext, Future}
import akka.pattern.ask
import modelsMethods.EventsMethods

import scala.concurrent.duration._

final case class PredictionLabels(attendingCount: Long,
                                  declinedCount: Long,
                                  interestedCount: Long,
                                  maybeCount: Long,
                                  noreplyCount: Long,
                                  organizersLikes: Seq[Long],
                                  placeCapacity: Long,
                                  placeLike: Long,
                                  artistsLikes: Seq[Long])

class MainController @Inject() (eventsDAO: EventsMethods, implicit val ec: ExecutionContext, actorSystem: ActorSystem)
    extends Controller with JsonImplicits {

  def findEvent(facebookId: String) = Action.async {
    eventsDAO.find(facebookId: String) map(e => Ok(Json.toJson(e)))
  }

  def predict = Action.async(parse.json) { request =>
    request.body.validate[PredictionLabels] match {
      case predictionLabels: JsSuccess[PredictionLabels] => Future.successful(Ok(Json.toJson(0)))
      case errors: JsError => Future.successful(BadRequest(errors.toString))
    }
  }

  def whatMyAddress = Action.async { implicit request =>
    val clusterAddressesActor = actorSystem.actorOf(Props[ClusterAddresses])
    implicit val timeout: akka.util.Timeout = 5.seconds

    clusterAddressesActor ? WhatMyAddress() map { address =>
      clusterAddressesActor ! PoisonPill
      Ok(address.asInstanceOf[String])
    }
  }
}
