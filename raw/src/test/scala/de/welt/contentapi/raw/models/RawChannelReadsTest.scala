package de.welt.contentapi.raw.models

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsArray, JsNumber, JsObject, JsString, Json}

class RawChannelReadsTest extends PlaySpec {

  "RawChannelReads" should {

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
        "stages" → JsArray(Seq(
          Json.toJson(channelStage)(RawWrites.rawChannelStageWrites)))
      ))
    }

    "read json" in new Fixture {
      val ch: RawChannel = j.result.validate[RawChannel](RawReads.rawChannelReads).get

      ch.id.path must be("le-path")
      val Some(stages) = ch.stages
      stages must be(Seq(RawChannelStageCustomModule(index = 1, module = "module", overrides = Some(Map("section" → "/foo/")))))

    }

    "have the data of the deprecated field stages in field stageConfiguration by using the Reads and Writes" in new Fixture {

      // JsObject -> RawChannel with only deprecated field "stages"
      val ch: RawChannel = j.result.validate[RawChannel](RawReads.rawChannelReads).get
      ch.stageConfiguration.flatMap(_.stages) must be(ch.stages)


      // RawChannel -> Json -> RawChannel with duplicated stages in stageConfiguration object
      private val json = Json.toJson(ch)(PartialRawChannelWrites.oneLevelOfChildren)
      private val reReadChannel = json.validate[RawChannel](RawReads.rawChannelReads).get
      reReadChannel.stageConfiguration.flatMap(_.stages) mustBe reReadChannel.stages
    }
  }

}
