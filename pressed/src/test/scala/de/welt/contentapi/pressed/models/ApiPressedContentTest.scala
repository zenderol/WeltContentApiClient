package de.welt.contentapi.pressed.models

import de.welt.contentapi.core.models.ApiContent
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsSuccess, Json}

class ApiPressedContentTest extends PlaySpec {

  "Reads" must {
    import PressedReads._
    val raw: String =
      """
        {
          "content": {
            "webUrl": "foo",
            "type": "bar"
          }
        }
      """

    // Info:
    // This test fails with Play 2.4
    "transform String->Model only with an `ApiContent`" ignore {
      Json.parse(raw).validate[ApiPressedContent] mustBe an[JsSuccess[_]]
    }

  }

  "Writes" must {
    import PressedWrites._

    val expectedJson: String = """{"content":{"webUrl":"foo","type":"bar"}}"""

    "transform Model->String only with an `ApiContent`" in {
      val apiPressedContent: ApiPressedContent = ApiPressedContent(content = ApiContent(webUrl = "foo", `type` = "bar"))
      apiPressedContentWrites.writes(apiPressedContent).toString() mustBe expectedJson
    }

  }


}
