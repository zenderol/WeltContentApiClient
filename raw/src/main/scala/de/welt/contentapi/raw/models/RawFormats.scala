package de.welt.contentapi.raw.models

import play.api.libs.functional.syntax._
import play.api.libs.json._

object RawFormats {

  import RawReads._
  import RawWrites._

  implicit lazy val rawChannelIdFormat: Format[RawChannelId] = Format[RawChannelId](rawChannelIdReads, rawChannelIdWrites)
  implicit lazy val rawChannelMetaRobotsTagFormat: Format[RawChannelMetaRobotsTag] = Format[RawChannelMetaRobotsTag](rawChannelMetaRobotsTagReads, rawChannelMetaRobotsTagWrites)
  implicit lazy val rawChannelMetadataFormat: Format[RawChannelMetadata] = Format[RawChannelMetadata](rawChannelMetadataReads, rawChannelMetadataWrites)
  implicit lazy val rawSectionReferenceFormat: Format[RawSectionReference] = Format[RawSectionReference](rawSectionReferenceReads, rawSectionReferenceWrites)
  implicit lazy val rawChannelHeaderFormat: Format[RawChannelHeader] = Format[RawChannelHeader](rawChannelHeaderReads, rawChannelHeaderWrites)
  implicit lazy val rawChannelContentConfigurationFormat: Format[RawChannelContentConfiguration] = Format[RawChannelContentConfiguration](rawChannelContentConfigurationReads, rawChannelContentConfigurationWrites)
  implicit lazy val rawChannelCommercialFormat: Format[RawChannelCommercial] = Format[RawChannelCommercial](rawChannelCommercialReads, rawChannelCommercialWrites)

  implicit lazy val rawChannelStageContentFormat: Format[RawChannelStageCustomModule] = Format[RawChannelStageCustomModule](rawChannelStageContentReads, rawChannelStageCustomModuleWrites)
  implicit lazy val rawChannelStageModuleFormat: Format[RawChannelStageModule] = Format[RawChannelStageModule](rawChannelStageModuleReads, rawChannelStageModuleWrites)
  implicit lazy val rawChannelStageCommercialFormat: Format[RawChannelStageCommercial] = Format[RawChannelStageCommercial](rawChannelStageCommercialReads, rawChannelStageCommercialWrites)
  implicit lazy val rawChannelStageFormat: Format[RawChannelStage] = Format[RawChannelStage](rawChannelStageReads, rawChannelStageWrites)
  implicit lazy val rawChannelThemeFormat: Format[RawChannelTheme] = Format[RawChannelTheme](rawChannelThemeReads, rawChannelThemeWrites)

  implicit lazy val rawMetadataFormat: Format[RawMetadata] = Format[RawMetadata](rawMetadataReads, rawMetadataWrites)
  implicit lazy val rawChannelConfigurationFormat: Format[RawChannelConfiguration] = Format[RawChannelConfiguration](rawChannelConfigurationReads, rawChannelConfigurationWrites)
}

object RawReads {
  private[models] def jsError(errorMessage: String, json: JsValue) =
    JsError(s"$errorMessage ${Json.prettyPrint(json)}")

  private[models] def jsErrorInvalidData(modelName: String, json: JsValue) =
    jsError(s"$modelName - Could not validate json [a constructor parameter is missing].", json)

  private[models] def jsErrorInvalidJson(json: JsValue) =
    jsError(s"expected js-object, but was:", json)

  implicit lazy val rawChannelIdReads: Reads[RawChannelId] = Json.reads[RawChannelId]
  implicit lazy val rawChannelMetaRobotsTagReads: Reads[RawChannelMetaRobotsTag] = Json.reads[RawChannelMetaRobotsTag]
  implicit lazy val rawChannelMetadataReads: Reads[RawChannelMetadata] = Json.reads[RawChannelMetadata]
  implicit lazy val rawSectionReferenceReads: Reads[RawSectionReference] = Json.reads[RawSectionReference]
  implicit lazy val rawChannelHeaderReads: Reads[RawChannelHeader] = Json.reads[RawChannelHeader]
  implicit lazy val rawChannelContentConfigurationReads: Reads[RawChannelContentConfiguration] = Json.reads[RawChannelContentConfiguration]

  implicit lazy val rawChannelTaboolaCommercialReads: Reads[RawChannelTaboolaCommercial] = new Reads[RawChannelTaboolaCommercial] {
    private lazy val defaults: RawChannelTaboolaCommercial = RawChannelTaboolaCommercial()
    override def reads(json: JsValue): JsResult[RawChannelTaboolaCommercial] = json match {
      case JsObject(underlying) ⇒ (for {
        showNews ← underlying.get("showNews").map(_.as[Boolean]).orElse(Some(defaults.showNews))
        showWeb ← underlying.get("showWeb").map(_.as[Boolean]).orElse(Some(defaults.showWeb))
        showNetwork ← underlying.get("showNetwork").map(_.as[Boolean]).orElse(Some(defaults.showNetwork))
      } yield JsSuccess(
        RawChannelTaboolaCommercial(
          showNews = showNews,
          showWeb = showWeb,
          showNetwork = showNetwork
        )
      )).getOrElse(jsErrorInvalidData("RawChannelTaboolaCommercial", json))
      case err@_ ⇒ jsErrorInvalidJson(err)
    }
  }

  implicit lazy val rawChannelCommercialReads = new Reads[RawChannelCommercial] {
    private lazy val defaults: RawChannelCommercial = RawChannelCommercial()
    override def reads(json: JsValue): JsResult[RawChannelCommercial] = json match {
      case JsObject(underlying) ⇒ (for {
        definesAdTag ← underlying.get("definesAdTag").map(_.as[Boolean]).orElse(Some(defaults.definesAdTag))
        definesVideoAdTag ← underlying.get("definesVideoAdTag").map(_.as[Boolean]).orElse(Some(defaults.definesVideoAdTag))
        showBiallo ← underlying.get("showBiallo").map(_.as[Boolean]).orElse(Some(defaults.showBiallo))
        contentTaboola ← underlying.get("contentTaboola").map(_.as[RawChannelTaboolaCommercial]).orElse(Some(defaults.contentTaboola))
      } yield JsSuccess(
        RawChannelCommercial(
          definesAdTag = definesAdTag,
          definesVideoAdTag = definesVideoAdTag,
          showBiallo = showBiallo,
          contentTaboola = contentTaboola
        )
      )).getOrElse(jsErrorInvalidData("RawChannelCommercial", json))
      case err@_ ⇒ jsErrorInvalidJson(err)
    }
  }

  implicit lazy val rawChannelStageContentReads: Reads[RawChannelStageCustomModule] = Json.reads[RawChannelStageCustomModule]
  implicit lazy val rawChannelStageModuleReads: Reads[RawChannelStageModule] = Json.reads[RawChannelStageModule]
  implicit lazy val rawChannelStageCommercialReads: Reads[RawChannelStageCommercial] = Json.reads[RawChannelStageCommercial]
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
  implicit lazy val rawChannelThemeReads: Reads[RawChannelTheme] = Json.reads[RawChannelTheme]

  implicit lazy val rawMetadataReads: Reads[RawMetadata] = Json.reads[RawMetadata]
  implicit lazy val rawChannelConfigurationReads: Reads[RawChannelConfiguration] = Json.reads[RawChannelConfiguration]
  implicit lazy val rawChannelReads: Reads[RawChannel] = Json.reads[RawChannel]
}

object RawWrites {
  implicit lazy val rawChannelIdWrites: Writes[RawChannelId] = Json.writes[RawChannelId]
  implicit lazy val rawChannelMetaRobotsTagWrites: Writes[RawChannelMetaRobotsTag] = Json.writes[RawChannelMetaRobotsTag]
  implicit lazy val rawChannelMetadataWrites: Writes[RawChannelMetadata] = Json.writes[RawChannelMetadata]
  implicit lazy val rawSectionReferenceWrites: Writes[RawSectionReference] = Json.writes[RawSectionReference]
  implicit lazy val rawChannelHeaderWrites: Writes[RawChannelHeader] = Json.writes[RawChannelHeader]
  implicit lazy val rawChannelContentConfigurationWrites: Writes[RawChannelContentConfiguration] = Json.writes[RawChannelContentConfiguration]
  implicit lazy val rawChannelTaboolaCommercialWrites: Writes[RawChannelTaboolaCommercial] = Json.writes[RawChannelTaboolaCommercial]
  implicit lazy val rawChannelCommercialWrites: Writes[RawChannelCommercial] = Json.writes[RawChannelCommercial]

  implicit lazy val rawChannelStageCustomModuleWrites: Writes[RawChannelStageCustomModule] = Json.writes[RawChannelStageCustomModule]
  implicit lazy val rawChannelStageModuleWrites: Writes[RawChannelStageModule] = Json.writes[RawChannelStageModule]
  implicit lazy val rawChannelStageCommercialWrites: Writes[RawChannelStageCommercial] = Json.writes[RawChannelStageCommercial]
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

  implicit lazy val rawChannelThemeWrites: Writes[RawChannelTheme] = Json.writes[RawChannelTheme]

  implicit lazy val rawMetadataWrites: Writes[RawMetadata] = Json.writes[RawMetadata]
  implicit lazy val rawChannelConfigurationWrites: Writes[RawChannelConfiguration] = Json.writes[RawChannelConfiguration]
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
        .getOrElse(jsErrorInvalidData("RawChannel[noChildren]", json))
      case err@_ ⇒ jsErrorInvalidJson(err)
    }
  }
}
