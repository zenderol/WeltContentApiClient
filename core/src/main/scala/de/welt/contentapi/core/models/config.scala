package de.welt.contentapi.core.models

import de.welt.contentapi.core.models.writes.PartialChannelWrites
import play.api.libs.functional.syntax._
import play.api.libs.json._

sealed trait Env

case object Preview extends Env

case object Live extends Env

case object UndefinedEnv extends Env

object Env {
  def apply(env: String): Env = env match {
    case "preview" ⇒ Preview
    case "live" ⇒ Live
    case _ ⇒ throw new IllegalArgumentException(s"Not a valid env: $env. Allowed values are 'preview' and 'live'")
  }
}

object reads {

  object FullChannelReads {

    import SimpleFormats._
    import StageFormats._
    import play.api.libs.functional.syntax._
    import play.api.libs.json._

    implicit lazy val channelReads: Reads[ApiChannel] = (
      (__ \ "id").read[ChannelId] and
        (__ \ "data").read[ApiChannelData] and
        (__ \ "stages").readNullable[Seq[Stage]] and
        (__ \ "parent").lazyRead(Reads.optionWithNull(channelReads)) and
        (__ \ "children").lazyRead(Reads.seq[ApiChannel](channelReads)) and
        (__ \ "hasChildren").read[Boolean] and
        (__ \ "lastModifiedDate").read[Long] and
        (__ \ "metadata").readNullable[ApiChannelMetadataNew]
      ) (ApiChannel)
  }

  object PartialChannelReads {

    import SimpleFormats._
    import StageFormats._

    implicit lazy val noChildrenReads: Reads[ApiChannel] = new Reads[ApiChannel] {
      override def reads(json: JsValue): JsResult[ApiChannel] = json match {
        case JsObject(underlying) ⇒ (for {
          id ← underlying.get("id").map(_.as[ChannelId])
          data ← underlying.get("data").map(_.as[ApiChannelData])
        } yield JsSuccess(
          ApiChannel(
            id = id,
            data = data,
            stages = underlying.get("stages").flatMap(_.asOpt[Seq[Stage]]),
            metadata = underlying.get("metadata").flatMap(_.asOpt[ApiChannelMetadataNew]),
            children = Seq.empty
          )))
          .getOrElse(JsError("Could not validate json [something is missing]. " + Json.prettyPrint(json)))

        case err@_ ⇒ JsError(s"expected js-object, but was $err")
      }
    }
  }

}

object writes {

  object FullChannelWrites {

    import SimpleFormats._
    import StageFormats._
    import play.api.libs.functional.syntax._
    import play.api.libs.json._

    implicit lazy val channelWrites: Writes[ApiChannel] = (
      (__ \ "id").write[ChannelId] and
        (__ \ "data").write[ApiChannelData] and
        (__ \ "stages").writeNullable[Seq[Stage]] and
        (__ \ "parent").lazyWrite(Writes.optionWithNull(PartialChannelWrites.writeChannelAsNull)) and // avoid loops
        (__ \ "children").lazyWrite(Writes.seq[ApiChannel](channelWrites)) and
        (__ \ "hasChildren").write[Boolean] and
        (__ \ "lastModifiedDate").write[Long] and
        (__ \ "metadata").writeNullable[ApiChannelMetadataNew]
      ) (unlift(ApiChannel.unapply))

  }

  object PartialChannelWrites {

    import SimpleFormats._
    import StageFormats._

    implicit lazy val noChildrenWrites = new Writes[ApiChannel] {
      override def writes(o: ApiChannel): JsValue = JsObject(Map(
        "id" → Json.toJson(o.id),
        "lastModifiedDate" → JsNumber(o.lastModifiedDate),
        "hasChildren" → JsBoolean(o.hasChildren),
        "data" → Json.toJson(o.data)
      ) ++ o.stages.map { stage ⇒ "stages" → Json.toJson(stage) }
        ++ o.metadata.map { metadata ⇒ "metadata" → Json.toJson(metadata) }
      )
    }

    implicit lazy val oneLevelOfChildren: Writes[ApiChannel] = (
      (__ \ "id").write[ChannelId] and
        (__ \ "data").write[ApiChannelData] and
        (__ \ "stages").writeNullable[Seq[Stage]] and
        (__ \ "parent").lazyWrite(Writes.optionWithNull(noChildrenWrites)) and
        (__ \ "children").lazyWrite(Writes.seq[ApiChannel](noChildrenWrites)) and
        (__ \ "hasChildren").write[Boolean] and
        (__ \ "lastModifiedDate").write[Long] and
        (__ \ "metadata").writeNullable[ApiChannelMetadataNew]
      ) (unlift(ApiChannel.unapply))

    implicit lazy val writeChannelAsNull: Writes[ApiChannel] = new Writes[ApiChannel] {
      override def writes(o: ApiChannel): JsValue = JsNull
    }
  }

}

object ChannelFormatNoChildren {
  implicit lazy val channelFormat: Format[ApiChannel] = Format(reads.PartialChannelReads.noChildrenReads, PartialChannelWrites.noChildrenWrites)
}

object SimpleFormats {

  import StageFormats._

  implicit lazy val idFormat: Format[ChannelId] = Json.format[ChannelId]
  implicit lazy val dataFormat: Format[ApiChannelData] = Json.format[ApiChannelData]
  implicit lazy val metaDataFormat: Format[ApiChannelMetadata] = Json.format[ApiChannelMetadata]
  implicit lazy val channelThemeFormat: Format[ApiChannelTheme] = Json.format[ApiChannelTheme]
  implicit lazy val adFormat: Format[ApiChannelAdData] = Json.format[ApiChannelAdData]
  implicit lazy val metaDataNewFormat: Format[ApiChannelMetadataNew] = Json.format[ApiChannelMetadataNew]

}


