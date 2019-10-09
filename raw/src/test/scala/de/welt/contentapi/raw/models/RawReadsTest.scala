package de.welt.contentapi.raw.models

import de.welt.contentapi.raw.models
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

    "ignore unknown properties for downward compatibility" in {
      val json: String =
        """{
          |  "header": {
          |    "logo": "foo.png"
          |  },
          |  "unknown-foo-bar": {}
          |}""".stripMargin
      Json.parse(json)
        .validate[RawChannelConfiguration](rawChannelConfigurationReads)
        .asOpt
        .flatMap(_.header) mustBe Some(RawChannelHeader(logo = Some("foo.png")))
    }

    "ignore known empty properties" in {
      val json: String =
        """{
          |  "header": {
          |    "logo": "foo.png"
          |  },
          |  "siteBuilding": {}
          |}""".stripMargin
      Json.parse(json)
        .validate[RawChannelConfiguration](rawChannelConfigurationReads)
        .asOpt
        .flatMap(_.header) mustBe Some(RawChannelHeader(logo = Some("foo.png")))
    }

    "ignore known empty properties with empty values" in {
      val json: String =
        """{
          |  "header": {
          |    "logo": "foo.png"
          |  },
          |  "siteBuilding": {"fields": {}}
          |}""".stripMargin
      Json.parse(json)
        .validate[RawChannelConfiguration](rawChannelConfigurationReads)
        .asOpt
        .flatMap(_.header) mustBe Some(RawChannelHeader(logo = Some("foo.png")))
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

    "map Some('') to None" in {
      val json: String = """{"logo": "", "slogan": "", "label": "", "sectionReferences": [], "hidden": false, "adIndicator": false}"""
      Json.parse(json)
        .validate[RawChannelHeader](rawChannelHeaderReads)
        .asOpt mustBe Some(RawChannelHeader(logo = None, slogan = None, label = None, sectionReferences = None))
    }

  }

  "RawChannelSiteBuildingReads" must {

    "create RawChannelSiteBuilding from empty json by using default constructor values" in {
      Json.parse(emptyJson)
        .validate[RawChannelSiteBuilding](rawChannelSiteBuildingReads)
        .asOpt mustBe Some(RawChannelSiteBuilding())
    }

    "fill optional fields" in {
      val json: String =
        """{
          |  "fields": {
          |    "header_slogan": "slogan-test"
          |  },
          |  "sub_navigation": [
          |    {
          |      "path": "www.welt.de",
          |      "label": "Click me"
          |    }
          |  ],
          |  "elements": [
          |    {
          |      "type": "mood",
          |      "assets": [
          |        {
          |          "type": "image",
          |          "fields": {
          |            "ratio": "1.77",
          |            "url": "www.welt.de"
          |          }
          |        }
          |      ]
          |    }
          |  ]
          |}""".stripMargin
      Json.parse(json)
        .validate[RawChannelSiteBuilding](rawChannelSiteBuildingReads)
        .asOpt mustBe Some(models.RawChannelSiteBuilding(
        fields = Some(Map("header_slogan" -> "slogan-test")),
        sub_navigation = Some(Seq(RawSectionReference(label = Some("Click me"), path = Some("www.welt.de")))),
        elements = Some(Seq(RawElement(id = RawChannelElement.IdDefault, `type` = "mood", assets = Some(List(RawAsset(`type` = "image", fields = Some(Map("ratio" -> "1.77", "url" -> "www.welt.de"))))))))
      )
      )
    }

    "map Some('') to None" in {
      val json: String =
        """{
          |  "sub_navigation": [],
          |  "elements": []
          |}""".stripMargin
      Json.parse(json)
        .validate[RawChannelSiteBuilding](rawChannelSiteBuildingReads)
        .asOpt mustBe Some(RawChannelSiteBuilding(sub_navigation = None, elements = None))
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
