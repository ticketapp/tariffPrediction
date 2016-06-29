import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.scalatest.{MustMatchers, WordSpecLike}
import org.specs2.mock.Mockito
import play.api.libs.concurrent.InjectedActorSupport
import websites.UnshortLinks.isAShortUrl

class UnshortLinksActorTest
  extends TestKit(ActorSystem("testSystem"))
    with WordSpecLike with MustMatchers with Mockito with InjectedActorSupport {

  "UnshortLink" must {

    "return true if the given url is a short url" in {
      isAShortUrl("bitly.com/byuS54") mustBe true
      isAShortUrl("http://bitly.com/byuS54") mustBe true
      isAShortUrl("https://bitly.com/byuS54") mustBe true
    }

    "return false if the given url is not a short url" in {
      isAShortUrl("http://ravepodcast.com") mustBe false
      isAShortUrl("ravepodcast.com") mustBe false
    }

//    "return an unshorted url given a short url" in {
//      val expectedUrl = "https://en.wikipedia.org/wiki/Iceland"
//      Await.result(unshortLinksActor ? Url("https://goo.gl/Plnzty"), 5 seconds) mustBe Some(expectedUrl)
//    }

//    "return the same url if there is no redirection or the url does not exist" in {
//      val url = "https://en.wikipedia.org/wiki/Iceland"
//      Await.result(unshortLink(url), 5 seconds) mustBe Some(url)
//    }
  }
}
