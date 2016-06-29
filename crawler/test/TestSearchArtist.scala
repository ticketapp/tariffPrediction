import APIs.FacebookAPI.Cover
import play.api.libs.json.Json
import testsHelper.Injectors.artistMethods
import testsHelper.testHelper
import websites.Websites.normalizeUrl

class TestSearchArtist extends testHelper {

  "SearchArtist" must {

    "normalize Facebook urls" in {
      val websites = Set(
        "facebook.com/cruelhand",
        "facebook.com/alexsmokemusic",
        "facebook.com/nemo.nebbia",
        "facebook.com/nosajthing",
        "facebook.com/kunamaze",
        "facebook.com/burningdownalaska",
        "facebook.com/diane-454634964631595/timeline",
        "facebook.com/beingasanocean",
        "facebook.com/theoceancollective",
        "facebook.com/woodwireproject",
        "facebook.com/lotfilafaceb",
        "facebook.com/loheem?fref=ts",
        "facebook.com/monoofjapan",
        "facebook.com/fitforakingband",
        "facebook.com/jp-manova",
        "facebook.com/solstafirice",
        "facebook.com/theamityafflictionofficial",
        "facebook.com/defeaterband",
        "facebook.com/musicseptembre?fref=ts",
        "facebook.com/paulatempleofficial",
        "Facebook.com/djvadim",
        "https://www.Facebook.com/djvadim?_rdr")

      val normalizedUrls = Set("fitforakingband",
        "loheem",
        "lotfilafaceb",
        "theamityafflictionofficial",
        "theoceancollective",
        "musicseptembre",
        "nosajthing",
        "burningdownalaska",
        "beingasanocean",
        "solstafirice",
        "cruelhand",
        "alexsmokemusic",
        "diane-454634964631595",
        "woodwireproject",
        "defeaterband",
        "paulatempleofficial",
        "monoofjapan",
        "jp-manova",
        "nemo.nebbia",
        "kunamaze",
        "djvadim",
        "djvadim")

      websites.flatMap(artistMethods.normalizeFacebookUrl) mustBe normalizedUrls
    }

    "aggregate the image path url with its offsets" in {
      val cover = Cover(offset_x = 1, offset_y = 200, source = "imageUrl")

      val expectedImageWithOffsets = """imageUrl\1\200"""

      artistMethods.aggregateImageAndOffset(Option(cover)) mustBe Some(expectedImageWithOffsets)
    }

    "read a web profiles json response from Soundcloud" in {
      artistMethods.readMaybeFacebookUrl(Json.parse(
        """[{"kind":"web-profile","id":19587164,"service":"facebook","title":null,
          |"url":"http://www.facebook.com/pages/Diane/454634964631595?ref=hl","username":"pages",
          |"created_at":"2013/06/02 14:39:25 +0000"}]""".stripMargin)) mustBe
        Some("facebook.com/pages/diane/454634964631595?ref=hl")
    }
  }
}

