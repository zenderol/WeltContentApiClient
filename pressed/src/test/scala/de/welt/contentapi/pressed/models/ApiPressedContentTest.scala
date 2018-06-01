package de.welt.contentapi.pressed.models

import de.welt.contentapi.core.models.ApiContent
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsSuccess, Json}

class ApiPressedContentTest extends PlaySpec {

  "Reads" must {
    val raw: String =
      """
        {
          "content": {
            "webUrl": "foo",
            "type": "bar"
          }
        }
      """

    "transform String->Model only with an `ApiContent`" in {
      Json.parse(raw).validate[ApiPressedContent](PressedReads.apiPressedContentReads) mustBe an[JsSuccess[_]]
    }

  }

  "Writes" must {
    import PressedWrites._

    val expectedJson: String = """{"content":{"webUrl":"foo","type":"bar"}}"""

    "transform Model->String only with an `ApiContent`" in {
      val apiPressedContent: ApiPressedContent = ApiPressedContent(content = ApiContent(webUrl = "foo", `type` = "bar"))
      apiPressedContentWrites.writes(apiPressedContent).toString() mustBe expectedJson
    }

    "write showFallbackAds to Json" in {
      val apiConfiguration = ApiConfiguration(commercial = Some(ApiCommercialConfiguration(
        showFallbackAds = Some(true)
      )))
      apiConfigurationWrites.writes(apiConfiguration).toString() must include("\"showFallbackAds\":true")
    }

  }

  "ApiPressedContent" should {
    import ApiPressedContentRoles._

    val main = ApiContent("main", "main")

    val mlt = ApiPressedContent(ApiContent("main", "mlt", roles = Some(List(MLT.name))))
    val related = ApiPressedContent(ApiContent("main", "related", roles = Some(List(Related.name))))
    val playlistRelated = ApiPressedContent(ApiContent("main", "playlist_related", roles = Some(List(Playlist.name, Related.name))))

    val apiPressedContent = ApiPressedContent(ApiContent("main", "main"), related = Some(List(mlt, related, playlistRelated)))

    "return related filtered by role" in {
      apiPressedContent.relatedByRole(MLT) must contain only mlt
    }

    "return multiple related if role matches" in {
      apiPressedContent.relatedByRole(Related) must contain allOf(related, playlistRelated)
    }

    "allow searching for multiple relations at once" in {
      apiPressedContent.relatedByRole(Playlist, MLT) must contain allOf(mlt, playlistRelated)
    }

    "return an empty list when query is empty" in {
      apiPressedContent.relatedByRole() mustBe empty
    }
  }
}
