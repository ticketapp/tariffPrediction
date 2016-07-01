package organizersDomain

import javax.inject.Inject

import akka.actor._
import akka.pattern.{Backoff, BackoffSupervisor}
import application.MainSupervisor.{EventsByOrganizersFinishedAt, IsFinished}
import com.google.inject.assistedinject.Assisted
import eventsDomain.EventMethods
import json.JsonHelper.FacebookRequestLimit
import org.joda.time.DateTime
import play.api.libs.concurrent.InjectedActorSupport

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object EventsByOrganizersSupervisor {
  final case class IncrementCounter()

  trait Factory {
    def apply(getEventsByOrganizersFactory: GetEventsByOrganizersActor.Factory,
              organizerMethods: OrganizerMethods,
              eventMethods: EventMethods,
              @Assisted claudeAddress: String,
              @Assisted startingOrganizerOffset: Long,
              ec: ExecutionContext): Actor
  }
}

class EventsByOrganizersSupervisor @Inject() (getEventsByOrganizersFactory: GetEventsByOrganizersActor.Factory,
                                              organizerMethods: OrganizerMethods,
                                              eventMethods: EventMethods,
                                              @Assisted claudeAddress: String,
                                              @Assisted startingOrganizerOffset: Long,
                                              implicit val ec: ExecutionContext)
    extends Actor with InjectedActorSupport with ActorLogging {
  import EventsByOrganizersSupervisor._

  var organizerOffset: Long = startingOrganizerOffset

  implicit val timeout: akka.util.Timeout = 5.seconds

  var lastTimeOffsetHaveBeenUpdated: DateTime = new DateTime()

  val getEventsByOrganizersProps = Props(getEventsByOrganizersFactory(organizerMethods, eventMethods, claudeAddress))

  val supervisor = BackoffSupervisor.props(
    Backoff.onFailure(
      getEventsByOrganizersProps,
      childName = "get-events-by-organizers",
      minBackoff = 3.seconds,
      maxBackoff = 10.minutes,
      randomFactor = 0.2))

  val supervisorRef = context.system.actorOf(supervisor, name = "events-by-organizer-supervisor")

  override def postStop = healthCheckScheduler.cancel()

  def healthCheckScheduler = context.system.scheduler.schedule(initialDelay = 1.minute, interval = 1.minute) {
    if (lastTimeOffsetHaveBeenUpdated.compareTo(new DateTime().minusMinutes(1)) < 0) {
      log info "The GetEventsByOrganizers Actor has not been responding for 1 minute and so will be restarted"
      context
        .actorSelection("/user/events-by-organizer-supervisor/get-events-by-organizers")
        .resolveOne()
        .map { actorRef =>
          organizerOffset += 1
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
      sender ! organizerOffset
      healthCheckScheduler
    }
  }

  def informEndSupervisor(): Unit = {
    log info "All events have been get for all organizers"
    healthCheckScheduler.cancel()
    context
      .actorSelection("/user/main-supervisor")
      .resolveOne()
      .map(actorRef => actorRef ! EventsByOrganizersFinishedAt(organizerOffset))
      .recover { case e => log error e.getMessage }
    supervisorRef ! PoisonPill
    self ! PoisonPill
  }

  def nextOrganizer(): Unit = {
    log info s"Organizer $organizerOffset done"
    lastTimeOffsetHaveBeenUpdated = new DateTime()
    organizerOffset += 1
    sender ! organizerOffset
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
