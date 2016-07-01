package artistsDomain

import javax.inject.Inject

import akka.actor._
import akka.pattern.{Backoff, BackoffSupervisor}
import application.MainSupervisor.{ArtistsAndGenresFinishedAt, IsFinished}
import com.google.inject.assistedinject.Assisted
import json.JsonHelper.FacebookRequestLimit
import org.joda.time.DateTime
import play.api.libs.concurrent.InjectedActorSupport
import websites.UnshortLinksFactory

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object ArtistsSupervisor {
  final case class IncrementCounter()

  trait Factory {
    def apply(ec: ExecutionContext,
              unshortLinksFactory: UnshortLinksFactory,
              artistMethods: ArtistMethods,
              claudeAddress: String,
              @Assisted startingEventOffset: Long): Actor
  }
}

class ArtistsSupervisor @Inject()(implicit val ec: ExecutionContext,
                                  unshortLinksFactory: UnshortLinksFactory,
                                  artistMethods: ArtistMethods,
                                  getArtistsAndGenresActorFactory: GetArtistsActor.Factory,
                                  @Assisted claudeAddress: String,
                                  @Assisted startingEventOffset: Long)
    extends Actor with InjectedActorSupport with ActorLogging {
  import ArtistsSupervisor._

  implicit val timeout: akka.util.Timeout = 5.seconds
  var eventOffset: Long =  startingEventOffset

  var lastTimeOffsetHaveBeenUpdated: DateTime = new DateTime()

  val getArtistProps = Props(getArtistsAndGenresActorFactory(
    ec = ec,
    unshortLinksFactory = unshortLinksFactory,
    artistMethods = artistMethods,
    claudeAddress = claudeAddress))

  val supervisor = BackoffSupervisor.props(
    Backoff.onFailure(
      getArtistProps,
      childName = "get-artists",
      minBackoff = 3.seconds,
      maxBackoff = 10.minutes,
      randomFactor = 0.2))

  val supervisorRef = context.system.actorOf(supervisor, name = "get-artists-supervisor")

  var healthCheckInstance = healthCheckScheduler

  override def postStop = healthCheckInstance.cancel()

  def healthCheckScheduler = context.system.scheduler.schedule(initialDelay = 1.minute, interval = 1.minute) {
    if (lastTimeOffsetHaveBeenUpdated.compareTo(new DateTime().minusMinutes(1)) < 0) {
      log info "The get-artists Actor has not been responding for 1 minute and so will be restarted"
      context
        .actorSelection("/user/get-artists-supervisor/get-artists")
        .resolveOne()
        .map { actorRef =>
          eventOffset += 1
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
      healthCheckInstance = healthCheckScheduler
      sender ! eventOffset
    }
  }

  def informEndSupervisor(): Unit = {
    log info "All events have been get for all organizers"
    healthCheckInstance.cancel()
    context
      .actorSelection("/user/main-supervisor")
      .resolveOne()
      .map(actorRef => actorRef ! ArtistsAndGenresFinishedAt(eventOffset))
      .recover { case e => log error e.getMessage }
    supervisorRef ! PoisonPill
    self ! PoisonPill
  }

  def nextOrganizer(): Unit = {
    log info s"Event $eventOffset done"
    lastTimeOffsetHaveBeenUpdated = new DateTime()
    eventOffset += 1
    sender ! eventOffset
  }

  def receive: Receive = {
    case IncrementCounter       => nextOrganizer()
    case IsFinished(true)       => informEndSupervisor()
    case FacebookRequestLimit() => pauseOneHour()
    case _                      => log error "Unhandled message"
  }

  def pause: Receive = {
    case _ => log error "Unhandled message"
  }
}
