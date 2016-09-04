package application

import javax.inject.Inject

import akka.actor.{ActorSystem, PoisonPill, Props}
import akka.pattern.ask
import application.ClusterAddresses.WhatMyAddress
import json.JsonImplicits
import modelsMethods.EventsMethods
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.tree.model.DecisionTreeModel
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.mvc.{Action, Controller}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.{implicitConversions, postfixOps}

final case class PredictionLabels(attendingCount: Long,
                                  declinedCount: Long,
                                  interestedCount: Long,
                                  maybeCount: Long,
                                  noreplyCount: Long,
                                  organizersLikes: Seq[Long],
                                  placeCapacity: Long,
                                  placeLike: Long,
                                  artistsLikes: Seq[Long])

class MainController @Inject() (extractData: ExtractData,
                                trainModel: TrainModel,
                                eventsDAO: EventsMethods,
                                implicit val ec: ExecutionContext,
                                actorSystem: ActorSystem)
    extends Controller with SparkCommons with JsonImplicits {

  def index = Action {
    val labeledPoints = extractData.extractRddLabeledPoints()

    val Array(trainingData, testData) = labeledPoints.randomSplit(Array(0.80, 0.20))

    val model = trainModel.trainModel(trainingData)

    model.save(sparkContext, "./model")

    val mse = testData.map(labeledPoint => Math.abs(model.predict(labeledPoint.features) - labeledPoint.label)).mean()

    val labeledPointsCount = labeledPoints.count()

    Ok(mse.toString + " for " + labeledPointsCount + " labeled points")
  }

  def findEvent(facebookId: String) = Action.async {
    eventsDAO.find(facebookId: String) map(e => Ok(Json.toJson(e)))
  }

  def predict = Action.async(parse.json) { request =>
    request.body.validate[PredictionLabels] match {
      case errors: JsError =>
        Future.successful(BadRequest(errors.toString))

      case predictionLabels: JsSuccess[PredictionLabels] =>
        val decisionTreeModel = DecisionTreeModel.load(sparkContext, "./model")
        val prediction = decisionTreeModel.predict(Vectors.fromJson(request.body.toString()))

        Future.successful(Ok(Json.toJson(prediction)))
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
