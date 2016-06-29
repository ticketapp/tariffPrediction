package logger

import akka.actor.{Actor, ActorLogging}

trait ActorsLoggerHelper {
  this: Actor with ActorLogging =>

  def logE(message: String)(implicit line: sourcecode.Line): Unit = log error line.value + ": " + message

  def logE(exception: Exception)(implicit line: sourcecode.Line): Unit =
    log error line.value + ": " + exception.getMessage

  def logE(throwable: Throwable)(implicit line: sourcecode.Line): Unit =
    log error line.value + ": " + throwable.getMessage
}
