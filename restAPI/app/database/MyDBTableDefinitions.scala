package database

import java.sql.{JDBCType, Timestamp}
import java.util.UUID

import com.vividsolutions.jts.geom.Geometry
import database.MyPostgresDriver.api._
import models._
import org.joda.time.DateTime
import slick.jdbc.{PositionedParameters, SetParameter}
import slick.model.ForeignKeyAction


case class EventArtistRelation(eventId: String, artistId: String)
case class EventPlaceRelation(eventId: String, placeId: String)
case class EventAddressRelation(eventId: String, addressId: Long)
case class EventOrganizerRelation(eventId: String, organizerId: String)
case class FrenchCity(city: String, geographicPoint: Geometry)

trait MyDBTableDefinitions {

  implicit object SetUUID extends SetParameter[UUID] {
    def apply(v: UUID, pp: PositionedParameters) { pp.setObject(v, JDBCType.BINARY.getVendorTypeNumber) }
  }

  def optionStringToSet(maybeString: Option[String]): Set[String] = maybeString match {
    case None => Set.empty
    case Some(string) => string.split(",").map(_.trim).filter(_.nonEmpty).toSet
  }

  implicit val jodaDateTimeMapping = {
    MappedColumnType.base[DateTime, Timestamp](
      dt => new Timestamp(dt.getMillis),
      ts => new DateTime(ts))
  }

  class Artists(tag: Tag) extends Table[Artist](tag, "artists") {
    def facebookId = column[String]("facebookid", O.PrimaryKey)
    def name = column[String]("name")
    def imagePath = column[Option[String]]("imagepath")
    def description = column[Option[String]]("description")
    def facebookUrl = column[String]("facebookurl")
    def websites = column[Option[String]]("websites")
    def hasTracks = column[Boolean]("hastracks", O.Default(false))
    def likes = column[Option[Long]]("likes")
    def country = column[Option[String]]("country")

    def * = (facebookId, name, imagePath, description, facebookUrl, websites, hasTracks, likes,
      country).shaped <> (
      { case (facebookId, name, imagePath, description, facebookUrl, websites, hasTracks, likes, country) =>
        Artist(facebookId, name, imagePath, description, facebookUrl, optionStringToSet(websites), hasTracks,
          likes, country)
      }, { artist: Artist =>
      Some((artist.facebookId, artist.name, artist.imagePath, artist.description, artist.facebookUrl,
        Option(artist.websites.mkString(",")), artist.hasTracks, artist.likes, artist.country))
    })
  }

  class Events(tag: Tag) extends Table[Event](tag, "events") {
    def id = column[Long]("event_id", O.PrimaryKey, O.AutoInc)
    def facebookId = column[String]("event_facebook_id", O.PrimaryKey)
    def name = column[String]("name")
    def description = column[Option[String]]("description")
    def startTime = column[DateTime]("starttime")
    def endTime = column[Option[DateTime]]("endtime")
    def ageRestriction = column[Int]("agerestriction")
    def tariffRange = column[Option[String]]("tariffrange")
    def ticketSellers = column[Option[String]]("ticketsellers")
    def imagePath = column[Option[String]]("imagepath")

    def * = (id.?, facebookId, name, description, startTime, endTime, ageRestriction, tariffRange,
      ticketSellers, imagePath) <> ((Event.apply _).tupled, Event.unapply)
  }

  class EventsPlaces(tag: Tag) extends Table[EventPlaceRelation](tag, "eventsplaces") {
    def eventId = column[String]("event_id")
    def placeFacebookUrl = column[String]("placefacebookurl")

    def * = (eventId, placeFacebookUrl) <> ((EventPlaceRelation.apply _).tupled, EventPlaceRelation.unapply)

    def aFK = foreignKey("event_id", eventId, events)(_.facebookId, onDelete = ForeignKeyAction.Cascade)
    def bFK =
      foreignKey("placefacebookurl", placeFacebookUrl, places)(_.facebookUrl, onDelete = ForeignKeyAction.Cascade)
  }

  class EventsAddresses(tag: Tag) extends Table[EventAddressRelation](tag, "eventsaddresses") {
    def eventId = column[String]("event_id")
    def addressId = column[Long]("addressid")

    def * = (eventId, addressId) <> ((EventAddressRelation.apply _).tupled, EventAddressRelation.unapply)

    def aFK = foreignKey("event_id", eventId, events)(_.facebookId, onDelete = ForeignKeyAction.Cascade)
    def bFK = foreignKey("addressid", addressId, addresses)(_.id, onDelete = ForeignKeyAction.Cascade)
  }

  class EventsOrganizers(tag: Tag) extends Table[EventOrganizerRelation](tag, "eventsorganizers") {
    def eventId = column[String]("event_id")
    def organizerUrl = column[String]("organizerurl")

    def * = (eventId, organizerUrl) <> ((EventOrganizerRelation.apply _).tupled, EventOrganizerRelation.unapply)

    def aFK = foreignKey("event_id", eventId, events)(_.facebookId, onDelete = ForeignKeyAction.Cascade)
    def bFK = foreignKey("organizerurl", organizerUrl, organizers)(_.facebookUrl, onDelete = ForeignKeyAction.Cascade)
  }

  class EventsArtists(tag: Tag) extends Table[EventArtistRelation](tag, "eventsartists") {
    def eventId = column[String]("event_id")
    def artistId = column[String]("artistid")

    def * = (eventId, artistId) <> ((EventArtistRelation.apply _).tupled, EventArtistRelation.unapply)

    def aFK = foreignKey("event_id", eventId, events)(_.facebookId, onDelete = ForeignKeyAction.Cascade)
    def bFK = foreignKey("artistid", artistId, artists)(_.facebookId, onDelete = ForeignKeyAction.Cascade)
  }

  class Places(tag: Tag) extends Table[Place](tag, "places") {
    def id = column[Long]("placeid", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def facebookId = column[String]("facebookid")
    def facebookUrl = column[String]("facebookurl")
    def description = column[Option[String]]("description")
    def websites = column[Option[String]]("websites")
    def capacity = column[Option[Int]]("capacity")
    def openingHours = column[Option[String]]("openinghours")
    def imagePath = column[Option[String]]("imagepath")
    def addressId = column[Option[Long]]("addressid")
    def likes = column[Option[Long]]("likes")

    def * = (id.?, name, facebookId, facebookUrl, description, websites, capacity, openingHours,
      imagePath, addressId, likes) <> ((Place.apply _).tupled, Place.unapply)
  }
  lazy val places = TableQuery[Places]

  class Organizers(tag: Tag) extends Table[Organizer](tag, "organizers") {
    def id = column[Long]("organizerid", O.PrimaryKey, O.AutoInc)
    def facebookId = column[String]("facebookid")
    def facebookUrl = column[String]("facebookurl")
    def name = column[String]("name")
    def description = column[Option[String]]("description")
    def addressId = column[Option[Long]]("addressid")
    def phone = column[Option[String]]("phone")
    def publicTransit = column[Option[String]]("publictransit")
    def websites = column[Option[String]]("websites")
    def verified = column[Boolean]("verified")
    def imagePath = column[Option[String]]("imagepath")
    def linkedPlaceUrl = column[Option[String]]("placeurl")
    def likes = column[Option[Long]]("likes")

    def * = (id.?, facebookId, facebookUrl, name, description, addressId, phone, publicTransit, websites, verified,
      imagePath, linkedPlaceUrl, likes) <> ((Organizer.apply _).tupled, Organizer.unapply)

    def address = foreignKey("addressFk", addressId, addresses)(_.id.?, onDelete = ForeignKeyAction.Cascade)
  }

  class Addresses(tag: Tag) extends Table[Address](tag, "addresses") {
    def id = column[Long]("addressid", O.PrimaryKey, O.AutoInc)
    def city = column[Option[String]]("city")
    def zip = column[Option[String]]("zip")
    def street = column[Option[String]]("street")
    def country = column[Option[String]]("country")

    def * = (id.?, city, zip, street, country) <> ((Address.apply _).tupled, Address.unapply)
  }

  class FrenchCities(tag: Tag) extends Table[FrenchCity](tag, "frenchcities") {
    def id = column[Long]("cityid", O.PrimaryKey, O.AutoInc)
    def city = column[String]("city")
    def geographicPoint = column[Geometry]("geographicpoint")

    def * = (city, geographicPoint) <> ((FrenchCity.apply _).tupled, FrenchCity.unapply)
  }

  class EventsCounts(tag: Tag) extends Table[Counts](tag, "eventscounts") {
    def eventFacebookId = column[String]("event_facebook_id", O.PrimaryKey)
    def attending_count = column[Long]("attending_count")
    def declined_count = column[Long]("declined_count")
    def interested_count = column[Long]("interested_count")
    def maybe_count = column[Long]("maybe_count")
    def noreply_count = column[Long]("noreply_count")

    def * = (
      eventFacebookId,
      attending_count,
      declined_count,
      interested_count,
      maybe_count,
      noreply_count) <>
      ((Counts.apply _).tupled, Counts.unapply)

    def afk = foreignKey("facebookid", eventFacebookId, events)(_.facebookId)
  }
  lazy val eventsCounts = TableQuery[EventsCounts]

  lazy val artists = TableQuery[Artists]
  lazy val events = TableQuery[Events]
  lazy val eventsAddresses = TableQuery[EventsAddresses]
  lazy val eventsOrganizers = TableQuery[EventsOrganizers]
  lazy val eventsArtists = TableQuery[EventsArtists]
  lazy val eventsPlaces = TableQuery[EventsPlaces]
  lazy val organizers = TableQuery[Organizers]
  lazy val addresses = TableQuery[Addresses]
  lazy val frenchCities = TableQuery[FrenchCities]
}
