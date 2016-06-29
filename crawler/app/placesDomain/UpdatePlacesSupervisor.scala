package placesDomain

import javax.inject.Inject

import akka.actor._
import akka.pattern.{Backoff, BackoffSupervisor}
import application.IsFinished
import application.MainSupervisor.UpdatePlacesFinishedAt
import com.google.inject.assistedinject.Assisted
import json.JsonHelper.FacebookRequestLimit
import org.joda.time.DateTime
import play.api.libs.concurrent.InjectedActorSupport
import play.api.libs.ws.WSClient

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object UpdatePlacesSupervisor {
  case object IncrementCounter

  trait Factory {
    def apply(updatePlacesActorFactory: UpdatePlacesActor.Factory,
              placeMethods: PlaceMethods,
              wSClient: WSClient,
              @Assisted claudeAddress: String,
              @Assisted startingPlaceOffset: Long,
              ec: ExecutionContext): Actor
  }
}

class UpdatePlacesSupervisor @Inject() (updatePlacesActorFactory: UpdatePlacesActor.Factory,
                                        placeMethods: PlaceMethods,
                                        wSClient: WSClient,
                                        @Assisted claudeAddress: String,
                                        @Assisted startingPlaceOffset: Long,
                                        implicit val ec: ExecutionContext)
    extends Actor with InjectedActorSupport with ActorLogging {
  import UpdatePlacesSupervisor._

  implicit val timeout: akka.util.Timeout = 5.seconds

  var placeOffset: Long = startingPlaceOffset

  var lastTimeOffsetHaveBeenUpdated: DateTime = new DateTime()

  val updatePlacesProps = Props(updatePlacesActorFactory(
    ec = ec,
    placeMethods = placeMethods,
    wSClient = wSClient,
    claudeAddress = claudeAddress))

  val supervisor = BackoffSupervisor.props(Backoff.onFailure(
    updatePlacesProps,
    childName = "update-places",
    minBackoff = 3.seconds,
    maxBackoff = 10.minutes,
    randomFactor = 0.2))

  val supervisorRef = context.system.actorOf(supervisor, name = "update-places-supervisor")

  def healthCheckScheduler = context.system.scheduler.schedule(initialDelay = 1.minute, interval = 1.minute) {
    if (lastTimeOffsetHaveBeenUpdated.compareTo(new DateTime().minusMinutes(1)) < 0) {
      log info "The UpdatePlaces Actor has not been responding for 1 minute and so will be restarted"

      context
        .actorSelection("/user/update-places-supervisor/update-places")
        .resolveOne()
        .map { actorRef =>
          placeOffset += 1
          actorRef ! new Exception
        }
        .recover { case e => log error e.getMessage }

    } else {
      log info "Seems to be alive"
    }
  }

  def pauseForAnHour(): Unit = {
    context.become(pause)
    log info "Scheduler will stop for an hour because it has hit the Facebook API limits"
    healthCheckScheduler.cancel()
    context.system.scheduler.scheduleOnce(1.hour) {
      lastTimeOffsetHaveBeenUpdated = new DateTime()
      context.unbecome
      sender ! placeOffset
      healthCheckScheduler
    }
  }

  def informEndSupervisor(): Unit = {
    log info "All events have been get for all organizers"
    healthCheckScheduler.cancel()
    context
      .actorSelection("/user/main-supervisor")
      .resolveOne()
      .map(actorRef => actorRef ! UpdatePlacesFinishedAt(placeOffset))
      .recover { case e => log error e.getMessage }
    supervisorRef ! PoisonPill
    self ! PoisonPill
  }

  def nextOrganizer(): Unit = {
    log info s"Place $placeOffset done"
    lastTimeOffsetHaveBeenUpdated = new DateTime()
    placeOffset += 1
    sender ! placeOffset
  }

  def receive: Receive = {
    case IncrementCounter       => nextOrganizer()
    case IsFinished(true)       => informEndSupervisor()
    case FacebookRequestLimit() => pauseForAnHour()
    case _                      => log error "Unhandled message"
  }

  def pause: Receive = {
    case _ => log error "Unhandled message"
  }
}
