package de.welt.contentapi.raw.models

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json

class RawReadsTest extends PlaySpec {

  import RawReads._

  private final val emptyJson = "{}"

  "RawChannelConfigurationReads" must {

    "create RawChannelConfiguration from empty json by using default constructor values" in {
      Json.parse(emptyJson)
        .validate[RawChannelConfiguration](rawChannelConfigurationReads)
        .asOpt mustBe Some(RawChannelConfiguration())
    }
  }

  "RawChannelCommercialReads" must {

    "create RawChannelCommercial from empty json by using default constructor values" in {
      Json.parse(emptyJson)
        .validate[RawChannelCommercial](rawChannelCommercialReads)
        .asOpt mustBe Some(RawChannelCommercial())
    }

    "ignore obsolete fields" in {
      val json: String = """{ "showBiallo": true }"""
      Json.parse(json)
        .validate[RawChannelCommercial](rawChannelCommercialReads)
        .asOpt mustBe Some(RawChannelCommercial())
    }

    "use constructor value for showFallbackAds" in {
      val json:String = """{ "definesAdTag": true,
        |"definesVideoAdTag": true}""".stripMargin
      Json.parse(json).validate[RawChannelCommercial](rawChannelCommercialReads).asOpt.map(_.showFallbackAds) mustBe Some(true)
    }

    "override constructor value showFallbackAds" in {
      val json:String = """{"definesAdTag": true,
                          |"definesVideoAdTag": true,
                          |"showFallbackAds": false}""".stripMargin
      val maybeChannel = Json.parse(json).validate[RawChannelCommercial](rawChannelCommercialReads).asOpt
      maybeChannel
        .map(_.showFallbackAds) mustBe Some(false)
    }

    "override constructor value disableAdvertisement" in {
      val json:String = """{"definesAdTag" : true,
                           |"definesVideoAdTag" : true,
                           |"disableAdvertisement" : true}""".stripMargin
      val maybeChannel = Json.parse(json).validate[RawChannelCommercial](rawChannelCommercialReads).asOpt
      maybeChannel
        .map(_.disableAdvertisement) mustBe Some(true)
    }
  }

  "RawChannelTaboolaCommercialReads" must {

    "create RawChannelTaboolaCommercial from empty json by using default constructor values" in {
      Json.parse(emptyJson)
        .validate[RawChannelTaboolaCommercial](rawChannelTaboolaCommercialReads)
        .asOpt mustBe Some(RawChannelTaboolaCommercial())
    }

  }

  "RawChannelHeaderReads" must {

    "create RawChannelHeader from empty json by using default constructor values" in {
      Json.parse(emptyJson)
        .validate[RawChannelHeader](rawChannelHeaderReads)
        .asOpt mustBe Some(RawChannelHeader())
    }

    "fill optional fields" in {
      val json: String = """{ "logo": "foo", "slogan": "foo", "label": "foo" }"""
      Json.parse(json)
        .validate[RawChannelHeader](rawChannelHeaderReads)
        .asOpt mustBe Some(RawChannelHeader(logo = Some("foo"), slogan = Some("foo"), label = Some("foo")))
    }

  }

  "RawChannelStage Reads" must {

    "parse unknown modules as a hidden RawChannelStageIgnored" in {
      val unknownModule =
        """
          |{
          |  "index": 0,
          |  "hidden": false,
          |  "type": "my-fance-new-module",
          |  "layout": "fancy-layout"
          |}
          |""".stripMargin
      val unknownStage: RawChannelStage = Json.parse(unknownModule)
        .validate[RawChannelStage](rawChannelStageReads)
        .asOpt
        .get
      unknownStage.`type` mustBe RawChannelStage.TypeUnknown
      unknownStage.hidden mustBe true
    }

  }

  "RawChannelStageCustomModule Reads" must {

    val customStageJson: String =
      """
        |{
        |  "index": 0,
        |  "module": "module-broadcasts",
        |  "hidden": false,
        |  "references": [],
        |  "overrides": {
        |    "sectionPath": "/mediathek/magazin/",
        |    "label": "MEDIATHEK",
        |    "limit": ""
        |  },
        |  "type": "custom-module"
        |}
      """.stripMargin

    "ignore empty override values to prevent parsing empty number values" in {
      val customStage: RawChannelStageCustomModule = Json.parse(customStageJson)
        .validate[RawChannelStageCustomModule](rawChannelStageCustomModuleReads)
        .asOpt
        .get

      customStage.unwrappedOverrides.get("limit") mustBe None
    }


  }

  "RawChannelStageCurated Reads" must {
    "work if defined values are missing in Json" in  {
      // while writing this test a new field was added (hideCuratedStageLabel) and is missing in the following json
      val json = """{
        |  "index": 0,
        |  "type": "curated",
        |  "hidden": false,
        |  "trackingName": "sondergruppe-1",
        |  "curatedSectionMapping": "frontpage",
        |  "curatedStageMapping": "sondergruppe-1",
        |  "layout": "oembed",
        |  "references": []
        |}
      """.stripMargin
      val stage: Option[RawChannelStageCurated] = Json.parse(json).validateOpt[RawChannelStageCurated](rawChannelStageCuratedReads).get
      stage.get.`type` mustBe RawChannelStage.TypeCurated
    }
  }
}
