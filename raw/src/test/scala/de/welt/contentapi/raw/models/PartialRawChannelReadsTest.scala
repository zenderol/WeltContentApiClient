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
          overrides = Some(Map("section" → "/foo/"))))(RawWrites.rawChannelStageCustomModuleWrites))
      val j = JsObject(Map(
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

    "read json with the no children reads" in new Fixture {
      val ch: RawChannel = j.result.validate[RawChannel](PartialRawChannelReads.noChildrenReads).get

      ch.id.path must be("le-path")
      val Some(stages) = ch.stageConfiguration.flatMap(_.stages)
      stages must be(Seq(RawChannelStageCustomModule(index = 1, module = "module", overrides = Some(Map("section" → "/foo/")))))

    }

  }

}
