package de.welt.contentapi.raw.models

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsNumber, JsObject, JsString, JsValue, Json}

class RawChannelReadsTest extends PlaySpec {

  "RawChannelReads" should {
    import RawWrites.rawChannelStageCustomModuleWrites
    import RawReads.rawChannelStageCustomModuleReads

    trait Fixture {
      val moduleValue = "module-for-raw-channel-reads"
      val overrides = Some(Map("section" → "/foo/"))
      val index = 1
      val originalTrackingName = Some("tracking-name")
      val originalLink = Some(RawSectionReference(path = Some("https://www.dick-butt.org")))

      val customModule: RawChannelStageCustomModule = RawChannelStageCustomModule(
        index = index,
        module = moduleValue,
        overrides = overrides,
        trackingName = originalTrackingName,
        link = originalLink
      )

      val stageAsJson: JsValue = Json.toJson(customModule)
      val rawStageFromJson: RawChannelStageCustomModule = Json.fromJson(stageAsJson).asOpt.orNull

      val rawChannelAsJsObject: JsObject = JsObject(Map(
        "id" → JsObject(Map(
          "path" → JsString("le-path"),
          "label" → JsString("id"),
          "escenicId" → JsNumber(1337)
        )),
        "config" → Json.toJson(RawChannelConfiguration())(RawWrites.rawChannelConfigurationWrites),
        "metadata" → Json.toJson(RawMetadata())(RawWrites.rawMetadataWrites),
        "stageConfiguration" → Json.toJson(RawChannelStageConfiguration(
          stages = Some(Seq(rawStageFromJson))
        ))(RawWrites.rawChannelStageConfigurationWrites)
      ))
    }

    "have `type` field in Json" in new Fixture {
      stageAsJson.toString must include(s""""type":"${RawChannelStage.TypeCustomModule}"""")
    }

    "RawChannelStage back from Json must have type" in new Fixture {
      rawStageFromJson.`type` mustBe RawChannelStage.TypeCustomModule
    }

    "read json" in new Fixture {
      val ch: RawChannel = rawChannelAsJsObject.result.validate[RawChannel](RawReads.rawChannelReads).get

      ch.id.path must be("le-path")
      val Some(stages) = ch.stageConfiguration.flatMap(_.stages)

      stages must be(Seq(RawChannelStageCustomModule(
        index = index,
        module = moduleValue,
        overrides = overrides,
        trackingName = originalTrackingName,
        link = originalLink
      )))

    }

  }

}
