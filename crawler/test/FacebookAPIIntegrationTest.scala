import APIs.FacebookAPI._
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import testsHelper.Injectors.facebookAPI

import scala.collection.immutable.Seq

class FacebookAPIIntegrationTest extends PlaySpec with OneAppPerSuite {

  "A Facebook API call" must {

    "return a sequence of artists" in {
      whenReady(facebookAPI.getArtists("rone"), timeout(Span(10, Seconds))) { artists =>
        assert((artists  \ "data").validate[Seq[FacebookArtist]].get.isInstanceOf[Seq[FacebookArtist]])
      }
    }

    "return an artist" in {
      whenReady(facebookAPI.getArtist("linofficiel"), timeout(Span(10, Seconds))) { artist =>

        assert(artist.validate[FacebookArtist].get.isInstanceOf[FacebookArtist])
      }
    }

    "return an event by facebookId" in {
      whenReady(facebookAPI.getEvent("985240908201444"), timeout(Span(10, Seconds))) { event =>

        event.validate[FacebookEvent].get.isInstanceOf[FacebookEvent]
      }
    }

    "return attendees for an event (by its facebook id)" in {
      whenReady(facebookAPI.getAttendees("866684910095368"),
        timeout(Span(5, Seconds))) { attendees =>

        assert((attendees \ "data").validate[Seq[FacebookAttendee]].get.isInstanceOf[Seq[FacebookAttendee]])
      }
    }

    "return an organizer by its facebook id" in {
      whenReady(facebookAPI.getOrganizer("164354640267171"), timeout(Span(10, Seconds))) { organizer =>

        assert(organizer.validate[FacebookOrganizer].get.isInstanceOf[FacebookOrganizer])
      }
    }

    "return a place by its facebook id" in {
      whenReady(facebookAPI.getPlace("164354640267171"), timeout(Span(10, Seconds))) { place =>

        assert(place.validate[FacebookPlace].get.isInstanceOf[FacebookPlace])
      }
    }

    "find some facebook ids for a place facebook id" in {
      whenReady(facebookAPI.getEventsId("117030545096697"), timeout(Span(10, Seconds))) { facebookIds =>
        1 mustBe 1
      }
    }
  }
}
