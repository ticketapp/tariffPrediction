package application

import com.google.inject.AbstractModule
import daoActors.{ArtistActor, EventActor, OrganizerActor, PlaceActor}
import play.api.libs.concurrent.AkkaGuiceSupport

class Modules extends AbstractModule with AkkaGuiceSupport {
  override def configure(): Unit = {
    bindActor[ArtistActor]("artist-actor")
    bindActor[EventActor]("event-actor")
    bindActor[PlaceActor]("place-actor")
    bindActor[OrganizerActor]("organizer-actor")
  }
}
