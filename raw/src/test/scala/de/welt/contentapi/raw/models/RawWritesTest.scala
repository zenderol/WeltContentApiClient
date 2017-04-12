package de.welt.contentapi.raw.models

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsValue, Json}

class RawWritesTest extends PlaySpec {

  import RawWrites._

  val stageLink = RawSectionReference(path = Some("https://www.dick-butt.org"))

  "RawChannelStageCustomModule Writes" must {

    sealed trait DefaultCustomModule {
      val actualRawCustomStage = RawChannelStageCustomModule(
        index = 0,
        module = "dick-butt",
        trackingName = Some("tracking-name"),
        link = Some(stageLink)
      )
      val expectedJson: String =
        """|{
           |  "index" : 0,
           |  "type" : "custom-module",
           |  "hidden" : false,
           |  "trackingName" : "tracking-name",
           |  "link" : {
           |    "path" : "https://www.dick-butt.org"
           |  },
           |  "module" : "dick-butt"
           |}""".stripMargin
    }

    sealed trait StandardCustomModule {
      val actualRawCustomStage = RawChannelStageCustomModule(
        index = 0,
        `type` = "dick-butt",
        hidden = true,
        trackingName = None,
        link = None,
        module = "dick-butt",
        references = Some(Seq(
          RawSectionReference(label = Some("ref-label"), path = Some("ref-path"))
        )),
        overrides = Some(Map(
          "sectionPath" → "/mediathek/magazin/",
          "label" → "MEDIATHEK",
          "limit" → ""
        ))
      )

      val expectedJson: String =
        """|{
           |  "index" : 0,
           |  "type" : "custom-module",
           |  "hidden" : true,
           |  "module" : "dick-butt",
           |  "references" : [ {
           |    "label" : "ref-label",
           |    "path" : "ref-path"
           |  } ],
           |  "overrides" : {
           |    "sectionPath" : "/mediathek/magazin/",
           |    "label" : "MEDIATHEK"
           |  }
           |}""".stripMargin
    }

    "generate valid JSON from default values (`Option.None` check)" in new DefaultCustomModule {
      val json: JsValue = Json.toJson[RawChannelStageCustomModule](actualRawCustomStage)(rawChannelStageCustomModuleWrites)

      Json.prettyPrint(json) mustBe expectedJson
    }

    """|always override the `type` with 'custom-module'
       |  Otherwise it's possible to override it with any value. (This is a glitch in PlayJson.Writes)""".stripMargin in new StandardCustomModule {
      val json: JsValue = Json.toJson[RawChannelStageCustomModule](actualRawCustomStage)(rawChannelStageCustomModuleWrites)

      val stageType: String = (json \ "type").as[String]
      stageType mustBe "custom-module"
    }

    """|filter all empty overrides values (`String.empty`)
       |  Prevents casting errors `"".as[Int]`""".stripMargin in new StandardCustomModule {
      val json: JsValue = Json.toJson[RawChannelStageCustomModule](actualRawCustomStage)(rawChannelStageCustomModuleWrites)

      Json.prettyPrint(json) mustBe expectedJson
    }

  }

  "RawChannelStageCommercial Writes" must {

    sealed trait DefaultCommercialStage {
      val actualRawCommercialStage = RawChannelStageCommercial(
        index = 0,
        format = "dick-butt",
        trackingName = Some("tracking-name"),
        link = Some(stageLink)
      )

      val expectedJson: String =
        """|{
           |  "index" : 0,
           |  "type" : "commercial",
           |  "hidden" : false,
           |  "trackingName" : "tracking-name",
           |  "link" : {
           |    "path" : "https://www.dick-butt.org"
           |  },
           |  "format" : "dick-butt"
           |}""".stripMargin
    }

    sealed trait StandardCommercialStage {
      val actualRawCommercialStage = RawChannelStageCommercial(
        index = 0,
        `type` = "dick-butt",
        hidden = true,
        trackingName = None,
        link = None,
        format = "dick-butt"
      )

      val expectedJson: String =
        """|{
           |  "index" : 0,
           |  "type" : "commercial",
           |  "hidden" : true,
           |  "format" : "dick-butt"
           |}""".stripMargin
    }

    "generate valid JSON from default values (`Option.None` check)" in new DefaultCommercialStage {
      val json: JsValue = Json.toJson[RawChannelStageCommercial](actualRawCommercialStage)(rawChannelStageCommercialWrites)

      Json.prettyPrint(json) mustBe expectedJson
    }

    """|always override the `type` with 'commercial'
       |  Otherwise it's possible to override it with any value. (This is a glitch in PlayJson.Writes)""".stripMargin in new StandardCommercialStage {
      val json: JsValue = Json.toJson[RawChannelStageCommercial](actualRawCommercialStage)(rawChannelStageCommercialWrites)

      val stageType: String = (json \ "type").as[String]
      stageType mustBe "commercial"
    }

  }

  "RawChannelStageCurated Writes" must {

    sealed trait DefaultCommercialStage {
      val actualRawCuratedStage = RawChannelStageCurated(
        index = 0,
        trackingName = Some("tracking-name"),
        link = Some(stageLink),
        curatedSectionMapping = "frontpage",
        curatedStageMapping = "hero",
        layout = None,
        label = None,
        logo = None
      )

      val expectedJson: String =
        """|{
           |  "index" : 0,
           |  "type" : "curated",
           |  "hidden" : false,
           |  "trackingName" : "tracking-name",
           |  "link" : {
           |    "path" : "https://www.dick-butt.org"
           |  },
           |  "curatedSectionMapping" : "frontpage",
           |  "curatedStageMapping" : "hero"
           |}""".stripMargin
    }

    sealed trait StandardCommercialStage {
      val actualRawCommercialStage = RawChannelStageCurated(
        index = 0,
        `type` = "dick-butt",
        hidden = true,
        trackingName = Some("tracking-name"),
        link = Some(stageLink),
        curatedSectionMapping = "frontpage",
        curatedStageMapping = "hero",
        layout = Some("curated-layout"),
        label = Some("curated-label"),
        logo = Some("curated-logo"),
        references = Some(Seq(
          RawSectionReference(label = Some("ref-label"), path = Some("ref-path"))
        ))
      )

      val expectedJson: String =
        """|{
           |  "index" : 0,
           |  "type" : "curated",
           |  "hidden" : true,
           |  "trackingName" : "tracking-name",
           |  "link" : {
           |    "path" : "https://www.dick-butt.org"
           |  },
           |  "curatedSectionMapping" : "frontpage",
           |  "curatedStageMapping" : "hero",
           |  "layout" : "curated-layout",
           |  "label" : "curated-label",
           |  "logo" : "curated-logo",
           |  "references" : [ {
           |    "label" : "ref-label",
           |    "path" : "ref-path"
           |  } ]
           |}""".stripMargin
    }

    "generate valid JSON from default values (`Option.None` check)" in new DefaultCommercialStage {
      val json: JsValue = Json.toJson[RawChannelStageCurated](actualRawCuratedStage)(rawChannelStageCuratedWrites)

      Json.prettyPrint(json) mustBe expectedJson
    }

    """|always override the `type` with 'curated'
       |  Otherwise it's possible to override it with any value. (This is a glitch in PlayJson.Writes)""".stripMargin in new StandardCommercialStage {
      val json: JsValue = Json.toJson[RawChannelStageCurated](actualRawCommercialStage)(rawChannelStageCuratedWrites)

      val stageType: String = (json \ "type").as[String]
      stageType mustBe "curated"
    }

  }

}
