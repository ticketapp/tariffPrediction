import org.joda.time.DateTime
import tariffsDomain.Tariff
import testsHelper.Injectors.tariffMethods
import testsHelper.testHelper

class TariffModelTest extends testHelper {

  val savedTariff = Tariff(
    tariffId = Some(10000),
    denomination = "test",
    eventId = 100,
    startTime = new DateTime("2040-08-24T14:00:00.000+02:00"),
    endTime = new DateTime("2040-09-24T14:00:00.000+02:00"),
    price = 10.0)

  "Prices" must {
    "be found" in {
      tariffMethods.findSortedTariffs("blabla 14.2132 eee 15,65465ee") mustBe Set.empty
      tariffMethods.findSortedTariffs("blabla 18 sqdlkj") mustBe Set.empty
      tariffMethods.findSortedTariffs("Canon en 2007 et Wich Side Are") mustBe Set.empty
      tariffMethods.findSortedTariffs("1er") mustBe Set.empty
      tariffMethods.findSortedTariffs("16 et 17") mustBe Set.empty
      tariffMethods.findSortedTariffs("entrée gratuite") mustBe Set(0)
      tariffMethods.findSortedTariffs("free entrance") mustBe Set(0)
      tariffMethods.findSortedTariffs("0€") mustBe Set.empty
      tariffMethods.findSortedTariffs("15 000 exemplaires") mustBe Set.empty
      tariffMethods.findSortedTariffs("VESTIAIRES: 1€/2€") mustBe Set.empty
      tariffMethods.findSortedTariffs("cloakroom: 2€/3€") mustBe Set.empty
      tariffMethods.findSortedTariffs("locker-room: 3€/4€") mustBe Set.empty
      tariffMethods.findSortedTariffs("changing-room: 4€/5€") mustBe Set.empty
      tariffMethods.findSortedTariffs("guardarropa: 5€/6€") mustBe Set.empty
      tariffMethods.findSortedTariffs("ion no no 6€ jlk ljk klj klj 7€") mustBe Set(6, 7)
      tariffMethods.findSortedTariffs("ion no no 7* / 9 €  jlk ljk klj klj") mustBe Set(9)
      tariffMethods.findSortedTariffs(
        "ion no no 6€ jlk ljk klj klj 145€ dsq q dqsdqsd q 4€") mustBe Set(4, 6, 145)
      tariffMethods.findSortedTariffs("4€/8€ qsdqsd") mustBe Set(4, 8)
      tariffMethods.findSortedTariffs("abc 4€-8€ qsdqsd") mustBe Set(4, 8)
      tariffMethods.findSortedTariffs("abc 5-9€ qsdqsd") mustBe Set(5, 9)
      tariffMethods.findSortedTariffs("Tarifs : 5/10 euros") mustBe Set(5, 10)
      tariffMethods.findSortedTariffs(
        "\n6€ jlk ljk klj klj 3  € dsq q dqsdqsd q 4€/8 € qsdqsd") mustBe Set(3.0, 4.0, 6.0, 8.0)
      tariffMethods.findSortedTariffs(
        "ion no no 6.8€ jlk ljk klj klj 3,45 € dsq q 16 ans minimum q 4€/5 € qsdqsd") mustBe Set(3.45, 4.0, 5.0, 6.8)
      tariffMethods.findSortedTariffs(
        "ion no no 15€/10€/7.5€ jlk ljk klj klj dsq q dqsdqsd q  qsdqsd") mustBe Set(7.5, 10.0, 15)
      tariffMethods.findSortedTariffs(" Minuit Bird — 14 euros Plein Tarif — 18 euros") mustBe Set(14, 18)
      tariffMethods.findSortedTariffs("✘ 24,50 € (tarif adhérent uniquement sur YURPLAN) / 26 € (étudiant chez " +
        "Yurplan) / 29,90 € dans les points de vente habituels / 33,00 € sur place ✘") mustBe
        Set(24.5, 26.0, 29.90, 33.0)
      tariffMethods.findSortedTariffs("✘ 24,50 € (tarif adhérent uniquement sur YURPLAN) / 26 € (étudiant chez " +
        "Yurplan) / 29,90 € dans les points de vente habituels / 33,00 € sur place ✘ \n►TARIF ADHERENT à 24,50€ chez " +
        "notre partenaire Yurplan :") mustBe Set(24.5, 26.0, 29.9, 33.0)
      tariffMethods.findSortedTariffs("tarif : 13 euros en prévente / 15 euros sur place") mustBe Set(13.0, 15.0)
      1 mustBe 1
    }
  }

  "Ticket sellers" must {
    "find ticket seller" in {
      val fnacTicket = tariffMethods.findTicketSellers(
        Set("lasasconcerts.fnacspectacles.com/place-spectacle/manifestation/musique-electronique-microphone-recordings-party-86273.htm"))
      val digitick = tariffMethods.findTicketSellers(Set("digitick.com"))

      fnacTicket mustBe
        Some("lasasconcerts.fnacspectacles.com/place-spectacle/manifestation/musique-electronique-microphone-recordings-party-86273.htm")
      digitick mustBe empty
    }
  }

  "The tariff range" must {
    "also be extracted" in {
      tariffMethods.extractTariffRange(Option("tarif : 13 euros en prévente / 15 euros sur place")) mustBe
        Option("13.0-15.0")
      tariffMethods.extractTariffRange(Option("ion no no 6€ jlk ljk klj klj 7€")) mustBe Option("6.0-7.0")
      tariffMethods.extractTariffRange(Option("ion no no 7* / 9 €  jlk ljk klj klj")) mustBe Option("9.0-9.0")
      tariffMethods.extractTariffRange(Option("")) mustBe None
      tariffMethods.extractTariffRange(Option.empty) mustBe None
    }
  }
}
