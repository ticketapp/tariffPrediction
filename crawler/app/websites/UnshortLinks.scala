package websites

import java.net.{ConnectException, UnknownHostException}
import javax.inject.Inject

import akka.actor.{Actor, ActorLogging}
import play.api.libs.ws.WSClient
import services.Utilities

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

object UnshortLinks {
  case class Url(url: String)
  case class Urls(urls: Set[String])

  trait Factory {
    def apply(ws: WSClient): Actor
  }

  def isAShortUrl(url: String): Boolean = {
    val urlWithoutHttp = url.stripPrefix("http://").stripPrefix("https://")
    if (urlWithoutHttp.toLowerCase.contains("www") || urlWithoutHttp.toLowerCase.contains("@")) {
      false
    } else if (urlWithoutHttp.startsWith("bit.ly") || urlWithoutHttp.startsWith("bit.do") ||
      urlWithoutHttp.startsWith("t.co") || urlWithoutHttp.startsWith("lnkd.in") || urlWithoutHttp.startsWith("db.tt") ||
      urlWithoutHttp.startsWith("qr.ae") || urlWithoutHttp.startsWith("adf.ly") ||
      urlWithoutHttp.startsWith("goo.gl") || urlWithoutHttp.startsWith("bitly.com") ||
      urlWithoutHttp.startsWith("cur.lv") || urlWithoutHttp.startsWith("tinyurlWithoutHttp.com") ||
      urlWithoutHttp.startsWith("ow.ly") || urlWithoutHttp.startsWith("adcrun.ch") ||
      urlWithoutHttp.startsWith("ity.im") || urlWithoutHttp.startsWith("q.gs") ||
      urlWithoutHttp.startsWith("viralurlWithoutHttp.com") || urlWithoutHttp.startsWith("is.gd") ||
      urlWithoutHttp.startsWith("vur.me") || urlWithoutHttp.startsWith("bc.vc") ||
      urlWithoutHttp.startsWith("twitthis.com") || urlWithoutHttp.startsWith("u.to") ||
      urlWithoutHttp.startsWith("j.mp") || urlWithoutHttp.startsWith("buzurlWithoutHttp.com") ||
      urlWithoutHttp.startsWith("cutt.us") || urlWithoutHttp.startsWith("u.bb") ||
      urlWithoutHttp.startsWith("yourlWithoutHttps.org") || urlWithoutHttp.startsWith("crisco.com") ||
      urlWithoutHttp.startsWith("x.co") || urlWithoutHttp.startsWith("prettylinkpro.com") ||
      urlWithoutHttp.startsWith("viralurlWithoutHttp.biz") || urlWithoutHttp.startsWith("adcraft.co") ||
      urlWithoutHttp.startsWith("virl.ws") || urlWithoutHttp.startsWith("scrnch.me") ||
      urlWithoutHttp.startsWith("filoops.info") || urlWithoutHttp.startsWith("vurlWithoutHttp.bz") ||
      urlWithoutHttp.startsWith("vzturlWithoutHttp.com") || urlWithoutHttp.startsWith("lemde.fr") ||
      urlWithoutHttp.startsWith("qr.net") || urlWithoutHttp.startsWith("1urlWithoutHttp.com") ||
      urlWithoutHttp.startsWith("tweez.me") || urlWithoutHttp.startsWith("7vd.cn") ||
      urlWithoutHttp.startsWith("v.gd") || urlWithoutHttp.startsWith("dft.ba") ||
      urlWithoutHttp.startsWith("aka.gr") || urlWithoutHttp.startsWith("tr.im")) {
      true
    } else {
      false
    }
  }
}

class UnshortLinks @Inject()(ws: WSClient) extends Actor with ActorLogging with Utilities {
  import UnshortLinks._

  implicit val timeout: akka.util.Timeout = 1.minute

  def unshortLinks(websites: Set[String]): Future[Set[String]] = websites
    .foldLeft(Future(Set.empty[String])) { case (previousFuture: Future[Set[String]], next) =>
      for {
        previous <- previousFuture
        nextResult <- unshortLink(next)
      } yield previous ++ nextResult
    }

  def unshortLink(url: String): Future[Option[String]] = {
    isAShortUrl(url) match {
      case true =>
        Try(ws.url(url).withFollowRedirects(false).head()) match {
          case Failure(e) =>
            log error s"Wrong url ($url)" + e
            Future.successful(Option(url))

          case Success(eventuallyResponse) =>
            eventuallyResponse.map { response =>
              response.status match {
                case code30x if code30x >= 300 && code30x < 400 => response.header("location")
                case _ => Option(url)
              }
            } recover {
              case unknownHostException: UnknownHostException =>
                log error s"Url $url does not exist"
                Option(url)

              case connectException: ConnectException =>
                log error s"$url unreachable"
                Option(url)

              case NonFatal(e) =>
                log error e.getMessage
                Option(url)
            }
        }

      case false =>
        Future.successful(Option(url))
    }
  }

  def receive = {
    case url: Url =>
      val senderCopy = sender
      unshortLink(url.url) map(unshortedUrl => senderCopy ! unshortedUrl)

    case urls: Urls =>
      val senderCopy = sender
      unshortLinks(urls.urls) map(unshortedUrls => senderCopy ! unshortedUrls)

    case _ =>
      log error "Unhandled message"
  }
}
