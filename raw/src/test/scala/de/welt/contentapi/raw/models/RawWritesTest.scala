package de.welt.contentapi.raw.models

import de.welt.contentapi.raw.models.RawReads.rawChannelStageConfiguredIdReads
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

    sealed trait MinimalCuratedStage {
      val actualRawCuratedStage = RawChannelStageCurated(
        index = 0,
        trackingName = Some("tracking-name"),
        link = Some(stageLink),
        curatedSectionMapping = "frontpage",
        curatedStageMapping = "hero",
        layout = None,
        label = None,
        logo = None,
        sponsoring = None
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

    sealed trait MaximalCuratedStage {
      val actualRawCuratedStage = RawChannelStageCurated(
        index = 0,
        `type` = "dick-butt", // overriding the type will be ignored
        hidden = true,
        trackingName = Some("tracking-name"),
        link = Some(stageLink),
        curatedSectionMapping = "frontpage",
        curatedStageMapping = "hero",
        layout = Some("curated-layout"),
        label = Some("curated-label"),
        logo = Some("curated-logo"),
        sponsoring = Some(RawSponsoringConfig(slogan = Some("slogan"))),
        references = Some(Seq(
          RawSectionReference(label = Some("ref-label"), path = Some("ref-path"))
        )),
        hideCuratedStageLabel = Some(true)
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
           |  } ],
           |  "hideCuratedStageLabel" : true
           |}""".stripMargin
    }

    "generate valid JSON from default values (`Option.None` check)" in new MinimalCuratedStage {
      val json: JsValue = Json.toJson[RawChannelStageCurated](actualRawCuratedStage)(rawChannelStageCuratedWrites)

      Json.prettyPrint(json) mustBe expectedJson
    }

    """|always override the `type` with 'curated'
       |  Otherwise it's possible to override it with any value. (This is a glitch in PlayJson.Writes)""".stripMargin in new MaximalCuratedStage {
      val json: JsValue = Json.toJson[RawChannelStageCurated](actualRawCuratedStage)(rawChannelStageCuratedWrites)

      val stageType: String = (json \ "type").as[String]
      stageType mustBe "curated"
    }

  }

  "RawChannelStageConfiguredId Reads and Writes" must {

      val ConfiguredIdStage = RawChannelStageConfiguredId(
        index = 0,
        `type` = RawChannelStage.TypeConfiguredId,
        hidden = false,
        trackingName = Some("tracking-name"),
        link = Some(stageLink),
        configuredId = "1234567890",
        label = Some("curated-label"),
        references = Some(Seq(
          RawSectionReference(label = Some("ref-label"), path = Some("ref-path"))
        ))
      )

      val expectedJson: String =
        s"""|{
           |  "index" : 0,
           |  "type" : "${RawChannelStage.TypeConfiguredId}",
           |  "hidden" : false,
           |  "trackingName" : "tracking-name",
           |  "link" : {
           |    "path" : "https://www.dick-butt.org"
           |  },
           |  "configuredId" : "1234567890",
           |  "label" : "curated-label",
           |  "references" : [ {
           |    "label" : "ref-label",
           |    "path" : "ref-path"
           |  } ]
           |}""".stripMargin


    "generate valid JSON from default values" in {
      val json: JsValue = Json.toJson[RawChannelStageConfiguredId](ConfiguredIdStage)(rawChannelStageConfiguredIdWrites)

      Json.prettyPrint(json) mustBe expectedJson
    }

    "construct the same object from Json" in {
      Json.parse(expectedJson).validate[RawChannelStageConfiguredId](rawChannelStageConfiguredIdReads).asOpt mustBe Some(ConfiguredIdStage)
    }

  }

  "RawChannelStageTracking Writes" must {

    sealed trait MinimalTrackingStage {
      val minimalStage = RawChannelStageTracking(
        index = 0,
        trackingName = None,
        link = None,
        layout = None,
        label = None,
        logo = None,
        reportName = "report-test-name"
      )

      val expectedJson: String =
        s"""|{
           |  "index" : 0,
           |  "type" : "${RawChannelStage.TypeTracking}",
           |  "hidden" : false,
           |  "reportName" : "report-test-name"
           |}""".stripMargin
    }

    sealed trait MaximalTrackingStage {
      val maximalStage = RawChannelStageTracking(
        index = 0,
        `type`= "dick-butt", // overriding the type will be ignored
        hidden = true,
        trackingName = Some("tracking-name"),
        link = Some(stageLink),
        layout = Some("layout"),
        label = Some("label"),
        logo = Some("logo"),
        references = Some(Seq(
          RawSectionReference(label = Some("ref-label"), path = Some("ref-path"))
        )),
        reportName = "report-test-name"
      )

      val expectedJson: String =
        s"""|{
           |  "index" : 0,
           |  "type" : "${RawChannelStage.TypeTracking}",
           |  "hidden" : true,
           |  "trackingName" : "tracking-name",
           |  "link" : {
           |    "path" : "https://www.dick-butt.org"
           |  },
           |  "layout" : "layout",
           |  "label" : "label",
           |  "logo" : "logo",
           |  "references" : [ {
           |    "label" : "ref-label",
           |    "path" : "ref-path"
           |  } ],
           |  "reportName" : "report-test-name"
           |}""".stripMargin
    }

    "generate valid JSON from minimal config" in new MinimalTrackingStage {
      val json: JsValue = Json.toJson[RawChannelStageTracking](minimalStage)(rawChannelStageTrackingWrites)
      Json.prettyPrint(json) mustBe expectedJson
    }
    "generate valid JSON from all possible values" in new MaximalTrackingStage {
      val json: JsValue = Json.toJson[RawChannelStageTracking](maximalStage)(rawChannelStageTrackingWrites)
      private val actual = Json.prettyPrint(json)
      actual mustBe expectedJson
    }

    "always have `type` 'tracking' " in new MaximalTrackingStage {
      val json: JsValue = Json.toJson[RawChannelStageTracking](maximalStage)(rawChannelStageTrackingWrites)
      val stageType: String = (json \ "type").as[String]
      stageType mustBe RawChannelStage.TypeTracking
    }

  }

}
