package de.welt.contentapi.raw.models

import org.scalatestplus.play.PlaySpec
import play.api.libs.json._

class PartialRawChannelReadsTest extends PlaySpec {

  "PartialRawChannelReads" should {

    trait Fixture {

      import RawReads.rawChannelStageCustomModuleReads
      import RawWrites.rawChannelStageCustomModuleWrites

      val originalModuleName = "module-for-partial-raw-channel-reads"
      val originalIndex = 99
      val originalOverrides = Some(Map("section" → "/foo/"))
      val originalTrackingName = Some("tracking-name")
      val originalLogo = Some("logo")
      val originalLink = Some(RawSectionReference(path = Some("https://www.dick-butt.org")))

      private val customModule = RawChannelStageCustomModule(
        index = originalIndex,
        module = originalModuleName,
        overrides = originalOverrides,
        trackingName = originalTrackingName,
        link = originalLink,
        logo = originalLogo
      )

      val rawStageAsJson: JsValue = Json.toJson(customModule)
      private val channelStage = Json.fromJson(rawStageAsJson).asOpt.orNull

      val rawChannelAsJsObject: JsObject = JsObject(Map(
        "id" → JsObject(Map(
          "path" → JsString("le-path"),
          "label" → JsString("id"),
          "escenicId" → JsNumber(1337)
        )),
        "config" → Json.toJson(RawChannelConfiguration())(RawWrites.rawChannelConfigurationWrites),
        "metadata" → Json.toJson(RawMetadata())(RawWrites.rawMetadataWrites),
        "stageConfiguration" → Json.toJson(RawChannelStageConfiguration(
          stages = Some(Seq(channelStage))
        ))(RawWrites.rawChannelStageConfigurationWrites)
      ))
    }

    "have `type` field in Json" in new Fixture {
      rawStageAsJson.toString must include(s""""type":"${RawChannelStage.TypeCustomModule}"""")
    }

    "read json with `no children reads`" in new Fixture {
      val ch: RawChannel = rawChannelAsJsObject.result.validate[RawChannel](PartialRawChannelReads.noChildrenReads).get

      ch.id.path must be("le-path")
      val Some(stages) = ch.stageConfiguration.flatMap(_.stages)
      stages must be(Seq(RawChannelStageCustomModule(
        index = originalIndex,
        module = originalModuleName,
        overrides = originalOverrides,
        trackingName = originalTrackingName,
        link = originalLink,
        logo = originalLogo
      )))

    }

  }

}
