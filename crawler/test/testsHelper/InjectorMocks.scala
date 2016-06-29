package testsHelper

import APIs.{FacebookAPI, FacebookAPIMock, SoundCloudAPI}
import akka.actor.ActorSystem
import artistsDomain.ArtistMethods
import genresDomain.GenreMethods
import org.scalatestplus.play.PlaySpec
import org.specs2.mock.Mockito
import organizersDomain.OrganizerMethodsMock
import placesDomain.PlaceMethodsMock
import play.api.db.slick.DatabaseConfigProvider
import play.api.i18n.MessagesApi
import play.api.libs.ws.WSClient
import websites.{UnshortLinks, WebsitesMock}

import scala.concurrent.ExecutionContext

trait InjectorMocks extends PlaySpec with Mockito {
  lazy val dbConfProviderMock = mock[DatabaseConfigProvider]
  lazy val wSClientMock = mock[WSClient]
  lazy val messageApiMock = mock[MessagesApi]
  lazy val genreMethodsMock = mock[GenreMethods]
  lazy val facebookAPIMock = new FacebookAPIMock(wSClientMock, ecMock)
  lazy val soundCloudAPIMock = mock[SoundCloudAPI]
  lazy val organizerMethodsMock = new OrganizerMethodsMock(facebookAPIMock)
  lazy val unshortLinksFactoryMock = mock[UnshortLinks.Factory]
  lazy val actorSystemMock = mock[ActorSystem]
  lazy val ecMock = mock[ExecutionContext]
  lazy val websitesMock = new WebsitesMock(unshortLinksFactoryMock, actorSystemMock, wSClientMock, ecMock)
  lazy val artistMethodsMock = mock[ArtistMethods]
  lazy val placeMethodsMock = new PlaceMethodsMock(facebookAPIMock, ecMock)
}
