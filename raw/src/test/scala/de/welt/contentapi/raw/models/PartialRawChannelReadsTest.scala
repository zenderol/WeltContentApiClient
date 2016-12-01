package de.welt.contentapi.raw.models

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsArray, JsNumber, JsObject, JsString, Json}

class PartialRawChannelReadsTest extends PlaySpec {

  "PartialRawChannelReads" should {

    trait Fixture {
      private val channelStage = RawChannelStage(Json.toJson(
        RawChannelStageCustomModule(
          index = 1,
          module = "module",
          sourceOverride = Some("source-override")))(RawWrites.rawChannelStageCustomModuleWrites))
      val j = JsObject(Map(
        "id" → JsObject(Map(
          "path" → JsString("le-path"),
          "label" → JsString("id"),
          "escenicId" → JsNumber(1337)
        )),
        "config" → Json.toJson(RawChannelConfiguration())(RawWrites.rawChannelConfigurationWrites),
        "metadata" → Json.toJson(RawMetadata())(RawWrites.rawMetadataWrites),
        "stages" → JsArray(Seq(
          Json.toJson(channelStage)(RawWrites.rawChannelStageWrites)))
      ))
    }

    "read json with the no childen reads" in new Fixture {
      val ch: RawChannel = j.result.validate[RawChannel](PartialRawChannelReads.noChildrenReads).get

      ch.id.path must be("le-path")
      val Some(stages) = ch.stages
      stages must be(Seq(RawChannelStageCustomModule(index = 1, module = "module", sourceOverride = Some("source-override"))))

    }
  }

}
