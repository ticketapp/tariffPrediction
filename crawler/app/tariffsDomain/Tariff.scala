package tariffsDomain

import logger.LoggerHelper
import org.joda.time.DateTime

import scala.util.{Success, Try}

final case class Tariff(tariffId: Option[Long] = None,
                        denomination: String,
                        eventId: Long,
                        startTime: DateTime,
                        endTime: DateTime,
                        price: BigDecimal)

class TariffMethods extends LoggerHelper {

  def extractTariffRange(maybeText: Option[String]): Option[String] = extractTariffRange(findSortedTariffs(maybeText))

  private def extractTariffRange(tariffs: Set[Double]): Option[String] = tariffs.isEmpty match {
    case false => Option(tariffs.min + "-" + tariffs.max)
    case true => None
  }

  def findSortedTariffs(text: String): Set[Double] = findTariffs(text).toVector.sorted.toSet

  private def findSortedTariffs(maybeText: Option[String]): Set[Double] = maybeText match {
    case None => Set.empty
    case Some(text) => findSortedTariffs(text)
  }

  private def findTariffs(text: String): Iterator[Double] = {
    val startOfLineOrSpace = "(^|\\s)"
    val startOfLineOrSpaceOrSeparator = "(^|\\s|/|-)"
    val numberBetween0And9999 = "(\\d{1,4})"
    val numberBetween0And9999AndDecimal = "(\\d{1,4}[\\.,]\\d{1,2})"
    val euros = "\\s*(€|e |euro)"
    val priceWithoutEuros = "(" + numberBetween0And9999 + "|" + numberBetween0And9999AndDecimal + ")"
    val freeEntranceRegex = "(entrée gratuite|entrée libre|free entrance)".r
    val oneTariffPattern = startOfLineOrSpaceOrSeparator + priceWithoutEuros + euros
    val twoTariffsPattern = startOfLineOrSpace + s"($priceWithoutEuros[$euros]?[-|/]$priceWithoutEuros)$euros"
    val oneOrTwoTariffs = s"($priceWithoutEuros[$euros]?[-|/]$priceWithoutEuros$euros|$priceWithoutEuros$euros)"
    val cloakroom = s"(vestiaire|changing-room|locker-room|cloakroom|guardarropa)[s]?[\\s]*[:]?[\\s]*$oneOrTwoTariffs"
    val textWithoutCloakroom = cloakroom.r.replaceAllIn(text.toLowerCase, "")

    val otherPrices = twoTariffsPattern.r.findAllIn(textWithoutCloakroom).matchData.map(_.group(0))

    val freeEntrancePrices = freeEntranceRegex.findFirstIn(textWithoutCloakroom) match {
      case Some(freeEntrance) => Iterator("0.0")
      case _ => Iterator.empty
    }

    val twoTariffsIterator = otherPrices.map { tariffs =>
      priceWithoutEuros.r.findAllIn(tariffs).matchData.map(_.group(0))
    }.flatten

    val oneTariffIterator = oneTariffPattern.r.findAllIn(textWithoutCloakroom).matchData.map(_.group(2))

    val tariffsWithoutZeros = fromIteratorStringToDouble(twoTariffsIterator ++ oneTariffIterator).filterNot(_ == 0.0)

    tariffsWithoutZeros ++ fromIteratorStringToDouble(freeEntrancePrices)
  }

  def fromIteratorStringToDouble(iterator: Iterator[String]): Iterator[Double] = iterator
    .map { tariff =>
      Try(tariff.replace(",", ".").toDouble) match {
        case Success(goodTariff) => Option(goodTariff)
        case _ => None
      }
    }.flatten

  def findTicketSellers(normalizedWebsites: Set[String]): Option[String] = {
    val websites = normalizedWebsites.filter(website =>
      website.contains("digitick") && website != "digitick.com" ||
        website.contains("weezevent") && website != "weezevent.com" ||
        website.contains("yurplan") && website != "yurplan.com" ||
        website.contains("eventbrite") && website != "eventbrite.fr" ||
        website.contains("ticketmaster") && website != "ticketmaster.fr" ||
        website.contains("fnacspectacles") && website != "fnacspectacles.com" ||
        website.contains("ticketnet") && website != "ticketnet.fr")

    if(websites.isEmpty) None
    else Option(websites.mkString(","))
  }
}
