package de.welt.contentapi.raw.models

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsValue, Json}

class PartialRawChannelWritesTest extends PlaySpec {

  "PartialRawChannelWrites" should {

    val allChannels: RawChannel = {
      RawChannel(RawChannelId(path = "/", escenicId = 1, label = "Frontpage"), children = List(
        RawChannel(RawChannelId(path = "/politik/", escenicId = 2, label = "Politik"), children = List(
          RawChannel(RawChannelId(path = "/wirtschaft/deutschland/", escenicId = 5, label = "Deutschland")),
          RawChannel(RawChannelId(path = "/wirtschaft/ausland/", escenicId = 6, label = "Ausland"))
        )),
        RawChannel(RawChannelId(path = "/wirtschaft/", escenicId = 3, label = "Wirtschaft")),
        RawChannel(RawChannelId(path = "/sport/", escenicId = 4, label = "Sport"))
      )
      )
    }

    "write json with all children of a channel`" in {
      val expectedJson: String =
        """{
          |  "id" : {
          |    "path" : "/",
          |    "label" : "Frontpage",
          |    "escenicId" : 1
          |  },
          |  "hasChildren" : true,
          |  "children" : [ {
          |    "id" : {
          |      "path" : "/politik/",
          |      "label" : "Politik",
          |      "escenicId" : 2
          |    },
          |    "hasChildren" : true,
          |    "children" : [ {
          |      "id" : {
          |        "path" : "/wirtschaft/deutschland/",
          |        "label" : "Deutschland",
          |        "escenicId" : 5
          |      },
          |      "hasChildren" : false,
          |      "children" : [ ]
          |    }, {
          |      "id" : {
          |        "path" : "/wirtschaft/ausland/",
          |        "label" : "Ausland",
          |        "escenicId" : 6
          |      },
          |      "hasChildren" : false,
          |      "children" : [ ]
          |    } ]
          |  }, {
          |    "id" : {
          |      "path" : "/wirtschaft/",
          |      "label" : "Wirtschaft",
          |      "escenicId" : 3
          |    },
          |    "hasChildren" : false,
          |    "children" : [ ]
          |  }, {
          |    "id" : {
          |      "path" : "/sport/",
          |      "label" : "Sport",
          |      "escenicId" : 4
          |    },
          |    "hasChildren" : false,
          |    "children" : [ ]
          |  } ]
          |}""".stripMargin

      val allChannelsAsJson: JsValue = Json.toJson(allChannels)(PartialRawChannelWrites.allChildrenWrites)

      Json.prettyPrint(allChannelsAsJson) mustBe expectedJson
    }
  }
}
