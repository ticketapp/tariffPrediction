package application

import artistsDomain.{ArtistsSupervisor, GetArtistsActor}
import com.google.inject.AbstractModule
import eventsDomain.GetFacebookEventActor
import net.codingwell.scalaguice.ScalaModule
import organizersDomain.{EventsByOrganizersSupervisor, GetEventsByOrganizersActor, GetOrganizerActor}
import placesDomain._
import play.api.libs.concurrent.AkkaGuiceSupport
import websites.UnshortLinks

class AllModules extends AbstractModule with AkkaGuiceSupport with ScalaModule {

  override def configure() = {
    bindActorFactory[UnshortLinks, UnshortLinks.Factory]
    bindActorFactory[ClaudeAddressActor, ClaudeAddressActor.Factory]

    bindActor[MainSupervisor]("main-supervisor")

    bindActor[GetFacebookEventActor]("get-facebook-event")
    bindActor[GetPlaceActor]("get-place")
    bindActor[GetOrganizerActor]("get-organizer")

    bindActorFactory[EventsByPlacesSupervisor, EventsByPlacesSupervisor.Factory]
    bindActorFactory[GetEventsByPlacesActor, GetEventsByPlacesActor.Factory]
    bindActorFactory[EventsByOrganizersSupervisor, EventsByOrganizersSupervisor.Factory]
    bindActorFactory[GetEventsByOrganizersActor, GetEventsByOrganizersActor.Factory]
    bindActorFactory[ArtistsSupervisor, ArtistsSupervisor.Factory]
    bindActorFactory[UpdatePlacesSupervisor, UpdatePlacesSupervisor.Factory]
    bindActorFactory[UpdatePlacesActor, UpdatePlacesActor.Factory]
    bindActorFactory[GetArtistsActor, GetArtistsActor.Factory]
  }
}
