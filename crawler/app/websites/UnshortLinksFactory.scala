package websites

import javax.inject.Inject

import akka.actor.{ActorSystem, Props}
import play.api.libs.ws.WSClient

class UnshortLinksFactory @Inject()(actorSystem: ActorSystem, unshortLinksFactory: UnshortLinks.Factory, ws: WSClient) {
  val unshortLinksActor = actorSystem.actorOf(Props(unshortLinksFactory(ws)))
}
