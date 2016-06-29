import addresses.{Address, GeographicPointTrait}
import attendees.Counts
import com.vividsolutions.jts.geom.Geometry
import eventsDomain.{Event, EventWithRelations}
import org.joda.time.DateTime
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}
import organizersDomain.{Organizer, OrganizerWithAddress}
import placesDomain.{Place, PlaceWithAddress}
import play.api.libs.json.Json
import testsHelper.Injectors._
import testsHelper.testHelper

class EventTest extends testHelper with GeographicPointTrait {
  "An event" must {

    "be read" in {
      val eventJson = Json.parse(
        """{"cover":
              {"offset_x":0,
              "offset_y":0,
              "source":"https://scontent.xx.fbcdn.net/v/t1.0-9/s720x720/1491728_10152081342841252_1874036730_n.jpg?oh=eacd96da83b8c61f71e5155b0d09cfed&oe=57C1291A",
              "id":"10152081342841252"},
            "description":"description",
            "name":"Cernunnos Pagan Festival 7",
            "start_time":"2014-02-23T14:00:00+0100",
            "end_time":"2014-02-23T22:30:00+0100",
            "owner":
              {"name":"Cernunnos Pagan Fest",
               "link":"https://www.facebook.com/facebookUrl",
               "id":"59836846251"},
            "attending_count":707,
            "declined_count":918,
            "interested_count":467,
            "maybe_count":467,
            "noreply_count":6286,
            "place":
              {"name":"La Machine du Moulin Rouge",
              "link":"https://www.facebook.com/facebookUrl",
              "location":
                {"city":"Paris",
                "country":"France",
                "latitude":48.88412,
                "longitude":2.33218,
                "street":"90 bd de Clichy",
                "zip":"75018"},
              "id":"106149749438924"},
            "id":"427110107393679"}""".stripMargin)

      val point: Geometry = stringToTryPoint("48.88412,2.33218").get
      val expectedEvent = EventWithRelations(
        event = Event(
          facebookId = "427110107393679",
          name = "Cernunnos Pagan Festival 7",
          geographicPoint = point,
          description = Some("description"),
          startTime = new DateTime("2014-02-23T14:00:00.000+01:00"),
          endTime = Option(new DateTime("2014-02-23T22:30:00.000+01:00")),
          imagePath = Some("https://scontent.xx.fbcdn.net/v/t1.0-9/s720x720/1491728_10152081342841252_1874036730_n.jpg?oh=eacd96da83b8c61f71e5155b0d09cfed&oe=57C1291A")),
        organizers = Vector(OrganizerWithAddress(Organizer(
          name = "name",
          facebookId = "facebookId",
          facebookUrl = "facebookUrl",
          geographicPoint = stringToTryPoint("-84,30").get))),
        places = Vector(PlaceWithAddress(
          place = Place(
            name = "La Machine du Moulin Rouge",
            facebookId = "106149749438924",
            facebookUrl = "https://www.facebook.com/facebookUrl",
            geographicPoint = stringToTryPoint("-84,30").get),
          maybeAddress = Some(Address(
            geographicPoint = point,
            city = Some("Paris"),
            zip = Some("75018"),
            street = Some("90 bd de Clichy"),
            country = Some("France"))))),
        counts = Some(Counts(
          eventFacebookId = "427110107393679",
          attending_count = 707,
          declined_count = 918,
          interested_count = 467,
          maybe_count = 467,
          noreply_count = 6286)))

      whenReady(eventMethods.facebookEventToEventWithRelations(eventJson), timeout(Span(6, Seconds))) { event =>

        event.get.geographicPoint mustBe point
        event mustBe Some(expectedEvent)
      }
    }
  }
}
