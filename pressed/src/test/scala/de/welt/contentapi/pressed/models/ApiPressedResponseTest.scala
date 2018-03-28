package de.welt.contentapi.pressed.models

import de.welt.contentapi.core.models.ApiContent
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsSuccess, Json}

class ApiPressedResponseTest extends PlaySpec {

  "ApiPressedContentResponse" must {

    trait Fixture {
      val content = ApiContent("le-url", "le-type")
      val response = ApiPressedContentResponse(
        result = ApiPressedContent(content),
        source = "test")
    }
    "Reads" must {
      trait ReadsFixture extends Fixture {
        val json =
          s"""
             |{"result":{
             |   "content":{
             |     "webUrl":"le-url",
             |     "type":"le-type"
             |    }
             |  },
             |  "source":"test",
             |  "total":1000,
             |  "pages":100,
             |  "pageSize":10,
             |  "currentPage":1,
             |  "orderBy":"random-ordering"
             |}
          """.stripMargin
      }
      "accept fixture" in new ReadsFixture {
        PressedReads.apiPressedContentResponseReads.reads(Json.parse(json)) mustBe an[JsSuccess[ApiPressedContentResponse]]
      }
      "contains expected data" in new ReadsFixture {
        val withPaging = response.copy(
          total = Some(1000),
          pages = Some(100),
          pageSize = Some(10),
          currentPage = Some(1),
          orderBy = Some("random-ordering")
        )
        PressedReads.apiPressedContentResponseReads.reads(Json.parse(json)).get mustBe withPaging
      }
    }

    "Writes" must {
      "write simple model to JSON" in new Fixture {
        PressedWrites.apiPressedContentResponseWrites.writes(response).toString() mustBe
          s"""
             |{"result":{
             |   "content":{
             |     "webUrl":"le-url",
             |     "type":"le-type"
             |    }
             |  },
             |  "source":"test"
             |}
          """.stripMargin.replaceAll("[\\s]", "")
      }
      "write paging model to JSON" in new Fixture {
        val withPaging = response.copy(
          total = Some(1000),
          pages = Some(100),
          pageSize = Some(10),
          currentPage = Some(1),
          orderBy = Some("random-ordering")
        )
        PressedWrites.apiPressedContentResponseWrites.writes(withPaging).toString() mustBe
          s"""
             |{"result":{
             |   "content":{
             |     "webUrl":"le-url",
             |     "type":"le-type"
             |    }
             |  },
             |  "source":"test",
             |  "total":1000,
             |  "pages":100,
             |  "pageSize":10,
             |  "currentPage":1,
             |  "orderBy":"random-ordering"
             |}
          """.stripMargin.replaceAll("[\\s]", "")
      }
    }
  }

}
