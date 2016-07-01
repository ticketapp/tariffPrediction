package websites

import javax.inject.Inject

import akka.actor.{ActorSystem, Props}
import akka.util.Timeout
import play.api.libs.ws.WSClient
import websites.UnshortLinks.Urls
import akka.pattern.ask

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

object Websites {
  val linkPattern = """((?:(http|https|Http|Https|rtsp|Rtsp):\/\/(?:(?:[a-zA-Z0-9\$\-\_\.\+\!\*\'\(\)\,\;\?\&\=]|(?:\%[a-fA-F0-9]{2})){1,64}(?:\:(?:[a-zA-Z0-9\$\-\_\.\+\!\*\'\(\)\,\;\?\&\=]|(?:\%[a-fA-F0-9]{2})){1,25})?\@)?)?((?:(?:[a-z@A-Z0-9][a-zA-Z0-9\-]{0,64}\.)+(?:(?:aero|arpa|asia|a[cdefgilmnoqrstuwxz])|(?:biz|b[abdefghijmnorstvwyz])|(?:cat|com|coop|c[acdfghiklmnoruvxyz])|d[ejkmoz]|(?:edu|e[cegrstu])|f[ijkmor]|(?:gov|g[abdefghilmnpqrstuwy])|h[kmnrtu]|(?:info|int|i[delmnoqrst])|(?:jobs|j[emop])|k[eghimnrwyz]|l[abcikrstuvy]|(?:mil|mobi|museum|m[acdghklmnopqrstuvwxyz])|(?:name|net|n[acefgilopruz])|(?:org|om)|(?:pro|p[aefghklmnrstwy])|qa|r[eouw]|s[abcdeghijklmnortuvyz]|(?:tel|travel|t[cdfghjklmnoprtvwz])|u[agkmsyz]|v[aceginu]|w[fs]|y[etu]|z[amw]))|(?:(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[0-9])))(?:\:\d{1,5})?)(\/(?:(?:[a-zA-Z0-9\;\/\?\:\@\&\=\#\~\-\.\+\!\*\'\(\)\,\_])|(?:\%[a-fA-F0-9]{2}))*)?(?:\b|$)""".r

  def retrieveNormalizedWebsites(text: String): Set[String] = retrieveWebsites(text) map normalizeUrl

  def retrieveNormalizedWebsites(maybeText: Option[String]): Set[String] =
    retrieveWebsites(maybeText) map normalizeUrl

  def retrieveWebsites(text: String): Set[String] = linkPattern.findAllIn(text).toSet

  def retrieveWebsites(maybeText: Option[String]): Set[String] = maybeText match {
    case Some(text) => linkPattern.findAllIn(text).toSet
    case None => Set.empty
  }

  def normalizeUrl(url: String): String =
    """(https?:\/\/(www\.)?)|(www\.)"""
      .r
      .replaceAllIn(url.toLowerCase, p => "")
      .stripSuffix("/")

  def normalizeUrl(maybeUrl: Option[String]): Option[String] = maybeUrl match {
    case Some(url) => Option(normalizeUrl(url))
    case _ => None
  }

  def normalizeMaybeUrl(maybeUrl: Option[String]): Option[String] = maybeUrl match {
    case Some(url) => Option(normalizeUrl(url))
    case _ => None
  }
}

class Websites @Inject() (unshortLinksFactory: UnshortLinks.Factory,
                          actorSystem: ActorSystem,
                          ws: WSClient,
                          implicit val ec: ExecutionContext) {
  import Websites._
  implicit val timeout: Timeout = 5.seconds
  val unshortLinksActor = actorSystem.actorOf(Props(unshortLinksFactory(ws)))

  def fromWebsiteStringToSet(maybeWebsitesNotFormatted: Option[String]): Future[Set[String]] = {
    maybeWebsitesNotFormatted match {
      case Some(websitesNotFormatted) =>
        val notUnshortedWebsites: Urls = Urls(retrieveNormalizedWebsites(websitesNotFormatted) map normalizeUrl)

        implicit val timeout: Timeout = 1.minute

        (unshortLinksActor ? notUnshortedWebsites).mapTo[Set[String]]

      case None =>
        Future.successful(Set.empty)
    }
  }

  def getUnshortedWebsites(websites: Set[String]): Future[Set[String]] =
    (unshortLinksActor ? Urls(websites)).mapTo[Set[String]]

  def getUnshortedNormalizedWebsites(websites: Set[String]): Future[Set[String]] = getUnshortedWebsites(websites)
    .map(eventuallyWebsites => eventuallyWebsites map normalizeUrl)

  def getUnshortedWebsites(text: String): Future[Set[String]] =
    (unshortLinksActor ? Urls(retrieveWebsites(text))).mapTo[Set[String]]

  def getUnshortedNormalizedWebsites(text: String): Future[Set[String]] =
    getUnshortedNormalizedWebsites(retrieveWebsites(text)).map(eventuallyWebsites => eventuallyWebsites map normalizeUrl)

  def getUnshortedWebsites(maybeText: Option[String]): Future[Set[String]] = maybeText match {
    case Some(text) => getUnshortedNormalizedWebsites(text)
    case _ => Future.successful(Set.empty)
  }

  def getUnshortedNormalizedWebsites(maybeText: Option[String]): Future[Set[String]] = getUnshortedWebsites(maybeText)
    .map(eventuallyWebsites => eventuallyWebsites map normalizeUrl)
}

class WebsitesMock @Inject() (unshortLinksFactory: UnshortLinks.Factory,
                              actorSystem: ActorSystem,
                              ws: WSClient,
                              ec: ExecutionContext)
    extends Websites(unshortLinksFactory: UnshortLinks.Factory,
                     actorSystem: ActorSystem,
                     ws: WSClient,
                     ec: ExecutionContext) {
  override def getUnshortedNormalizedWebsites(maybeText: Option[String]): Future[Set[String]] =
    Future.successful(Set("website"))
}
