package placesDomain

import javax.inject.Inject

import akka.actor._
import akka.pattern.{Backoff, BackoffSupervisor}
import application.MainSupervisor.{EventsByPlacesFinishedAt, IsFinished}
import com.google.inject.assistedinject.Assisted
import eventsDomain.EventMethods
import json.JsonHelper.FacebookRequestLimit
import org.joda.time.DateTime
import play.api.libs.concurrent.InjectedActorSupport
import play.api.libs.ws.WSClient

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object EventsByPlacesSupervisor {
  final case class IncrementCounter()

  trait Factory {
    def apply(getEventsByPlacesActorFactory: GetEventsByPlacesActor.Factory,
              eventMethods: EventMethods,
              placeMethods: PlaceMethods,
              wSClient: WSClient,
              @Assisted claudeAddress: String,
              @Assisted startingPlaceOffset: Long,
              ec: ExecutionContext): Actor
  }
}

class EventsByPlacesSupervisor @Inject() (getEventsByPlacesActorFactory: GetEventsByPlacesActor.Factory,
                                          eventMethods: EventMethods,
                                          placeMethods: PlaceMethods,
                                          wSClient: WSClient,
                                          @Assisted claudeAddress: String,
                                          @Assisted startingPlaceOffset: Long,
                                          implicit val ec: ExecutionContext)
    extends Actor with InjectedActorSupport with ActorLogging {
  import EventsByPlacesSupervisor._

  implicit val timeout: akka.util.Timeout = 5.seconds

  var placeOffset: Long = startingPlaceOffset

  var lastTimeOffsetHaveBeenUpdated: DateTime = new DateTime()

  val getEventsByPlacesProps = Props(getEventsByPlacesActorFactory(
    ec = ec,
    eventMethods = eventMethods,
    placeMethods = placeMethods,
    wSClient = wSClient,
    claudeAddress = claudeAddress))

  val supervisor = BackoffSupervisor.props(Backoff.onFailure(
    getEventsByPlacesProps,
    childName = "get-events-by-places",
    minBackoff = 3.seconds,
    maxBackoff = 10.minutes,
    randomFactor = 0.2))

  val supervisorRef = context.system.actorOf(supervisor, name = "events-by-place-supervisor")

  var healthCheckInstance = healthCheckScheduler

  override def postStop = healthCheckInstance.cancel()

  def healthCheckScheduler: Cancellable = context
    .system
    .scheduler
    .schedule(initialDelay = 1.minute, interval = 1.minute) {
      if (lastTimeOffsetHaveBeenUpdated.compareTo(new DateTime().minusMinutes(1)) < 0) {
        log info "The GetEventsByPlaces Actor has not been responding for 1 minute and so will be restarted"

        context
          .actorSelection("/user/events-by-place-supervisor/get-events-by-places")
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

  def pauseOneHour(): Unit = {
    context.become(pause)
    healthCheckInstance.cancel()
    log info "Scheduler will stop for an hour because it has hit the Facebook API limits"
    context.system.scheduler.scheduleOnce(1.hour) {
      lastTimeOffsetHaveBeenUpdated = new DateTime()
      context.unbecome
      sender ! placeOffset
      healthCheckInstance = healthCheckScheduler
    }
  }

  def informEndSupervisor(): Unit = {
    log info "All events have been get for all places"
    healthCheckInstance.cancel()
    context
      .actorSelection("/user/main-supervisor")
      .resolveOne()
      .map(actorRef => actorRef ! EventsByPlacesFinishedAt(placeOffset))
      .recover { case e => log error e.getMessage }
    supervisorRef ! PoisonPill
    self ! PoisonPill
  }

  def nextPlace(): Unit = {
    log info s"Place $placeOffset done"
    lastTimeOffsetHaveBeenUpdated = new DateTime()
    placeOffset += 1
    sender ! placeOffset
  }

  def receive: Receive = {
    case IncrementCounter       => nextPlace()
    case IsFinished(true)       => informEndSupervisor()
    case FacebookRequestLimit() => pauseOneHour()
    case _                      => log error "Unhandled message"
  }

  def pause: Receive = {
    case _ => log error "Unhandled message"
  }
}
