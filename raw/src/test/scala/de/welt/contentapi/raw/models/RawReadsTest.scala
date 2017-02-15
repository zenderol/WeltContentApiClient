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
      val unknownModule = """{
                            |  "index": 0,
                            |  "hidden": false,
                            |  "type": "my-fance-new-module",
                            |  "layout": "fancy-layout"
                            |}"""".stripMargin
      val unknownStage = Json.parse(unknownModule)
        .validate[RawChannelStage](rawChannelStageReads)
        .asOpt
        .get
      unknownStage.`type` mustBe RawChannelStage.unknown
      unknownStage.hidden mustBe true
    }

  }


}
