package application

import javax.inject.Inject

import play.api.mvc.{Action, Controller}

import scala.language.{implicitConversions, postfixOps}

class MainController @Inject() (trainModel: TrainModel) extends Controller {

  def index = Action {

    Ok
  }
}
