package testsHelper

import APIs.{FacebookAPI, SoundCloudAPI}
import addresses.SearchGeographicPoint
import akka.actor.ActorSystem
import application.AllModules
import artistsDomain.ArtistMethods
import eventsDomain.EventMethods
import organizersDomain.OrganizerMethods
import play.api.Mode
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import tariffsDomain.TariffMethods
import websites.UnshortLinks

object Injectors extends InjectorMocks {
  lazy val appBuilder = new GuiceApplicationBuilder()
  lazy val appWithoutActorsBuilder = new GuiceApplicationBuilder()
    .in(Mode.Test)
    .disable[AllModules]
    .build()
  lazy val injector = appBuilder.injector()
  lazy val injectorWithoutActors = appWithoutActorsBuilder.injector
  lazy val wSClientWithoutActors = injectorWithoutActors.instanceOf[WSClient]
  lazy val wSClient = wSClientWithoutActors//injector.instanceOf[WSClient]
  lazy val actorSystem = ActorSystem()
  lazy val ec = scala.concurrent.ExecutionContext.Implicits.global

  lazy val unshortLinksFactory = injector.instanceOf[UnshortLinks.Factory]

  lazy val facebookAPI = new FacebookAPI(wSClientWithoutActors, ec)
  lazy val soundCloudAPI = new SoundCloudAPI(wSClient, ec)
  lazy val searchGeographicPoint = new SearchGeographicPoint(wSClient)
  lazy val artistMethods = new ArtistMethods(
    soundCloudAPIMock,
    websitesMock,
    facebookAPIMock)
  lazy val organizerMethods = new OrganizerMethods(facebookAPIMock)
  lazy val tariffMethods = new TariffMethods
  lazy val eventMethods = new EventMethods(
    organizerMethodsMock,
    artistMethodsMock,
    tariffMethods,
    placeMethodsMock,
    searchGeographicPoint,
    ec,
    websitesMock,
    facebookAPIMock)
}
