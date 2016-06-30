package application

import javax.inject.Inject

import play.api.mvc.{Action, Controller}

import scala.language.{implicitConversions, postfixOps}

class MainController @Inject() (extractData: ExtractData, trainModel: TrainModel) extends Controller {

  def index = Action {
    val labeledPoints = extractData.extractRddLabeledPoints()

    val Array(trainingData, testData) = labeledPoints.randomSplit(Array(0.80, 0.20))

    val model = trainModel.trainModel(trainingData)

    val mse = testData.map(labeledPoint => Math.abs(model.predict(labeledPoint.features) - labeledPoint.label)).mean()

    val labeledPointsCount = labeledPoints.count()

    Ok(mse.toString + " for " + labeledPointsCount + " labeled points")
  }
}
