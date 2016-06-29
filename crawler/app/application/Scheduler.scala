package application

import javax.inject.Inject

import addresses.SearchGeographicPoint
import artistsDomain.ArtistMethods
import eventsDomain.EventMethods
import organizersDomain.OrganizerMethods
import placesDomain.PlaceMethods
import services.Utilities
import tracksDomain.TrackMethods

final case class IsFinished(value: Boolean)

class Scheduler @Inject()(eventMethods: EventMethods,
                          organizerMethods: OrganizerMethods,
                          artistMethods: ArtistMethods,
                          trackMethods: TrackMethods,
                          placeMethods: PlaceMethods,
                          searchGeographicPoint: SearchGeographicPoint) extends Utilities {

/*
  def findTracksForArtistsOneByOne(offset: Long = 0): Unit = artistMethods.findSinceOffset(numberToReturn = 1, offset) map {
    artists =>
    artists.headOption match {
      case Some(artist) =>
        val tracksEnumerator = artistMethods.getArtistTracks(PatternAndArtist(artist.artist.name, artist))
        trackMethods.saveTracksEnumerator(tracksEnumerator)

        Thread.sleep(4000)
        findTracksForArtistsOneByOne(offset + 1)

      case _ =>
        Logger.info("Scheduler.findTracksForArtistsOneByOne: DONE :)")
    }
  }

  def updateGeographicPointOfPlaces50By50(offset: Long = 0): Unit = placeMethods.findSinceOffset(offset, numberToReturn = 50) map {
    places =>
      updateGeographicPointOfPlaces(places)
      places.size match {
        case 50 =>
          Thread.sleep(1500)
          updateGeographicPointOfPlaces50By50(offset + 50)

        case _ =>
      }
  }

  def updateGeographicPointOfPlaces(places: Seq[PlaceWithAddress]): Seq[Any] = places map { place =>
    place.place.geographicPoint match {
      case geoPoint if geoPoint == antarcticPoint =>
        place.maybeAddress match {
          case Some(address) => getGeoPointOfPlaceIfAbsent(place, address)
          case _ =>
        }

      case _ =>
    }
  }

  def updateGeographicPointOfOrganizers50By50(offset: Long = 0): Unit =
    organizerMethods.findSinceOffset(offset, numberToReturn = 50) map {
    organizers =>
      updateGeographicPointOfOrganizers(organizers)
      organizers.size match {
        case 50 =>
          Thread.sleep(1500)
          updateGeographicPointOfPlaces50By50(offset + 50)

        case _ =>
      }
  }

  def updateGeographicPointOfOrganizers(organizers: Seq[OrganizerWithAddress]): Seq[Any] = organizers map { organizer =>
    organizer.organizer.geographicPoint match {
      case geoPoint if geoPoint == antarcticPoint =>
        organizer.address match {
          case Some(address) => getGeoPointOfOrganizerIfAbsent(organizer, address)
          case _ =>
        }

      case _ =>
    }
  }

  def updateGeographicPointOfEvents50By50(offset: Long = 0): Unit =
    eventMethods.findSinceOffset(offset, numberToReturn = 50) map { events =>
      updateGeographicPointOfEvents(events)

      events.size match {
        case 50 =>
          Thread.sleep(1500)
          updateGeographicPointOfEvents50By50(offset + 50)

        case _ =>
      }
  }

  def updateGeographicPointOfEvents(events: Seq[EventWithRelations]): Seq[Any] = events map { event =>
    event.event.geographicPoint match {
      case geoPoint if geoPoint == antarcticPoint =>
        event.addresses.headOption match {
          case Some(address) =>
            getGeoPointOfEventIfAbsent(event, address)

          case _ =>
            if (event.places.nonEmpty) {
              event.places.head.maybeAddress match {
                case Some(address) => getGeoPointOfEventIfAbsent(event, address)
                case _ =>
              }
            }
        }

      case _ =>
    }
  }

  def getGeoPointOfPlaceIfAbsent(place: PlaceWithAddress, address: Address): Future[Any] = address.geographicPoint match {
    case geoPoint if geoPoint != antarcticPoint =>
      val updatedPlace = place.place.copy(geographicPoint = geoPoint)
      placeMethods.update(updatedPlace)

    case _ =>
      searchGeographicPoint.getGeographicPoint(address, retry = 3) map { addressWithMaybeGeographicPoint =>
        addressWithMaybeGeographicPoint.geographicPoint match {
          case geoPoint if geoPoint != antarcticPoint =>
            addressMethods.update(addressWithMaybeGeographicPoint)
            placeMethods.update(place.place.copy(geographicPoint = geoPoint))

          case _ =>
            None
        }
      } recover { case NonFatal(e) => log(e) }
  }
  
  def getGeoPointOfOrganizerIfAbsent(organizer: OrganizerWithAddress, address: Address): Future[Any] = address.geographicPoint match {
    case geoPoint if geoPoint != antarcticPoint =>
      val updatedOrganizer = organizer.organizer.copy(geographicPoint = geoPoint)
      organizerMethods.update(updatedOrganizer)

    case _ =>
      searchGeographicPoint.getGeographicPoint(address, retry = 3) map { addressWithMaybeGeographicPoint =>
        addressWithMaybeGeographicPoint.geographicPoint match {
          case geoPoint if geoPoint != antarcticPoint =>
            addressMethods.update(addressWithMaybeGeographicPoint)
            organizerMethods.update(organizer.organizer.copy(geographicPoint = geoPoint))

          case _ =>
            None
        }
      } recover { case NonFatal(e) => log(e) }
  }
  
  def getGeoPointOfEventIfAbsent(event: EventWithRelations, address: Address): Future[Any] = address.geographicPoint match {
    case geoPoint if geoPoint != antarcticPoint =>
      val updatedEvent = event.event.copy(geographicPoint = geoPoint)
      eventMethods.update(updatedEvent)

    case _ =>
      searchGeographicPoint.getGeographicPoint(address, retry = 3) map { addressWithMaybeGeographicPoint =>
        addressWithMaybeGeographicPoint.geographicPoint match {
          case geoPoint if geoPoint != antarcticPoint =>
            addressMethods.update(addressWithMaybeGeographicPoint)
            eventMethods.update(event.event.copy(geographicPoint = geoPoint))

          case _ =>
            None
        }
      } recover { case NonFatal(e) => log(e) }
  }*/
}
