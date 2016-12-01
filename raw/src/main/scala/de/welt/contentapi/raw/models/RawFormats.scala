package de.welt.contentapi.raw.models

import play.api.libs.functional.syntax._
import play.api.libs.json._

object RawFormats {

  import RawReads._
  import RawWrites._

  implicit lazy val rawChannelIdFormat = Format[RawChannelId](rawChannelIdReads, rawChannelIdWrites)
  implicit lazy val rawChannelMetaRobotsTagFormat = Format[RawChannelMetaRobotsTag](rawChannelMetaRobotsTagReads, rawChannelMetaRobotsTagWrites)
  implicit lazy val rawChannelMetadataFormat = Format[RawChannelMetadata](rawChannelMetadataReads, rawChannelMetadataWrites)
  implicit lazy val rawSectionReferenceFormat = Format[RawSectionReference](rawSectionReferenceReads, rawSectionReferenceWrites)
  implicit lazy val rawChannelHeaderFormat = Format[RawChannelHeader](rawChannelHeaderReads, rawChannelHeaderWrites)
  implicit lazy val rawChannelCommercialFormat = Format[RawChannelCommercial](rawChannelCommercialReads, rawChannelCommercialWrites)

  implicit lazy val rawChannelStageContentFormat = Format[RawChannelStageCustomModule](rawChannelStageContentReads, rawChannelStageCustomModuleWrites)
  implicit lazy val rawChannelStageModuleFormat = Format[RawChannelStageModule](rawChannelStageModuleReads, rawChannelStageModuleWrites)
  implicit lazy val rawChannelStageCommercialFormat = Format[RawChannelStageCommercial](rawChannelStageCommercialReads, rawChannelStageCommercialWrites)
  implicit lazy val rawChannelStageFormat = Format[RawChannelStage](rawChannelStageReads, rawChannelStageWrites)
  implicit lazy val rawChannelThemeFormat = Format[RawChannelTheme](rawChannelThemeReads, rawChannelThemeWrites)

  implicit lazy val rawMetadataFormat = Format[RawMetadata](rawMetadataReads, rawMetadataWrites)
  implicit lazy val rawChannelConfigurationFormat = Format[RawChannelConfiguration](rawChannelConfigurationReads, rawChannelConfigurationWrites)
}

object RawReads {
  implicit lazy val rawChannelIdReads = Json.reads[RawChannelId]
  implicit lazy val rawChannelMetaRobotsTagReads = Json.reads[RawChannelMetaRobotsTag]
  implicit lazy val rawChannelMetadataReads = Json.reads[RawChannelMetadata]
  implicit lazy val rawSectionReferenceReads = Json.reads[RawSectionReference]
  implicit lazy val rawChannelHeaderReads = Json.reads[RawChannelHeader]
  implicit lazy val rawChannelCommercialReads = Json.reads[RawChannelCommercial]

  implicit lazy val rawChannelStageContentReads = Json.reads[RawChannelStageCustomModule]
  implicit lazy val rawChannelStageModuleReads = Json.reads[RawChannelStageModule]
  implicit lazy val rawChannelStageCommercialReads = Json.reads[RawChannelStageCommercial]
  implicit lazy val rawChannelStageReads = new Reads[RawChannelStage] {
    override def reads(json: JsValue): JsResult[RawChannelStage] = {
      (json \ "type").as[String] match {
        case RawChannelStage.customModule =>
          Json.fromJson[RawChannelStageCustomModule](json)
        case RawChannelStage.module =>
          Json.fromJson[RawChannelStageModule](json)
        case RawChannelStage.commercial =>
          Json.fromJson[RawChannelStageCommercial](json)
      }
    }
  }
  implicit lazy val rawChannelThemeReads = Json.reads[RawChannelTheme]

  implicit lazy val rawMetadataReads = Json.reads[RawMetadata]
  implicit lazy val rawChannelConfigurationReads = Json.reads[RawChannelConfiguration]
  implicit lazy val rawChannelReads = Json.reads[RawChannel]
}

object RawWrites {
  implicit lazy val rawChannelIdWrites = Json.writes[RawChannelId]
  implicit lazy val rawChannelMetaRobotsTagWrites = Json.writes[RawChannelMetaRobotsTag]
  implicit lazy val rawChannelMetadataWrites = Json.writes[RawChannelMetadata]
  implicit lazy val rawSectionReferenceWrites = Json.writes[RawSectionReference]
  implicit lazy val rawChannelHeaderWrites = Json.writes[RawChannelHeader]
  implicit lazy val rawChannelCommercialWrites = Json.writes[RawChannelCommercial]

  implicit lazy val rawChannelStageCustomModuleWrites = Json.writes[RawChannelStageCustomModule]
  implicit lazy val rawChannelStageModuleWrites = Json.writes[RawChannelStageModule]
  implicit lazy val rawChannelStageCommercialWrites = Json.writes[RawChannelStageCommercial]
  implicit lazy val rawChannelStageWrites = new Writes[RawChannelStage] {
    override def writes(o: RawChannelStage): JsValue = o match {
      case r: RawChannelStageCustomModule =>
        Json.toJson(r)(rawChannelStageCustomModuleWrites)
      case m: RawChannelStageModule =>
        Json.toJson(m)(rawChannelStageModuleWrites)
      case c: RawChannelStageCommercial =>
        Json.toJson(c)(rawChannelStageCommercialWrites)
    }
  }

  //Json.writes[RawChannelStage]
  implicit lazy val rawChannelThemeWrites = Json.writes[RawChannelTheme]

  implicit lazy val rawMetadataWrites = Json.writes[RawMetadata]
  implicit lazy val rawChannelConfigurationWrites = Json.writes[RawChannelConfiguration]
  //  implicit lazy val rawChannelWrites = Json.writes[RawChannel]
}

object FullRawChannelWrites {

  import RawWrites._

  implicit lazy val channelWrites: Writes[RawChannel] = (
    (__ \ "id").write[RawChannelId] and
      (__ \ "config").write[RawChannelConfiguration] and
      (__ \ "stages").writeNullable[Seq[RawChannelStage]] and
      (__ \ "metadata").write[RawMetadata] and
      (__ \ "parent").lazyWrite(Writes.optionWithNull(PartialRawChannelWrites.writeChannelAsNull)) and // avoid loops
      (__ \ "children").lazyWrite(Writes.seq[RawChannel](channelWrites)) and
      (__ \ "hasChildren").write[Boolean]
    ) (unlift(RawChannel.unapply))

}

object PartialRawChannelWrites {

  import RawWrites._

  implicit lazy val noChildrenWrites: Writes[RawChannel] = new Writes[RawChannel] {
    override def writes(o: RawChannel): JsValue = {

      JsObject(Map(
        "id" → Json.toJson(o.id),
        "hasChildren" → JsBoolean(o.hasChildren),
        "config" → Json.toJson(o.config),
        "metadata" → Json.toJson(o.metadata)
      ) ++ o.stages.map { stages ⇒ "stages" → Json.toJson(stages) }
      )
    }
  }

  implicit lazy val oneLevelOfChildren: Writes[RawChannel] = (
    (__ \ "id").write[RawChannelId] and
      (__ \ "config").write[RawChannelConfiguration] and
      (__ \ "stages").writeNullable[Seq[RawChannelStage]] and
      (__ \ "metadata").write[RawMetadata] and
      (__ \ "parent").lazyWrite(Writes.optionWithNull(noChildrenWrites)) and
      (__ \ "children").lazyWrite(Writes.seq[RawChannel](noChildrenWrites)) and
      (__ \ "hasChildren").write[Boolean]
    ) (unlift(RawChannel.unapply))

  implicit lazy val writeChannelAsNull: Writes[RawChannel] = new Writes[RawChannel] {
    override def writes(o: RawChannel): JsValue = JsNull
  }
}

object PartialRawChannelReads {

  import RawReads._

  implicit lazy val noChildrenReads: Reads[RawChannel] = new Reads[RawChannel] {
    override def reads(json: JsValue): JsResult[RawChannel] = json match {
      case JsObject(underlying) ⇒ (for {
        id ← underlying.get("id").map(_.as[RawChannelId])
        config ← underlying.get("config").map(_.as[RawChannelConfiguration])
        metadata ← underlying.get("metadata").map(_.as[RawMetadata])
      } yield JsSuccess(
        RawChannel(
          id = id,
          config = config,
          metadata = metadata,
          stages = underlying.get("stages").flatMap(_.asOpt[Seq[RawChannelStage]]),
          children = Seq.empty
        )))
        .getOrElse(JsError("Could not validate json [something is missing]. " + Json.prettyPrint(json)))

      case err@_ ⇒ JsError(s"expected js-object, but was $err")
    }
  }
}