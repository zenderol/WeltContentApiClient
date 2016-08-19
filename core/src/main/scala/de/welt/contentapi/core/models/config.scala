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

    implicit lazy val channelReads: Reads[Channel] = (
      (__ \ "id").read[ChannelId] and
        (__ \ "data").read[ChannelData] and
        (__ \ "stages").readNullable[Seq[Stage]] and
        (__ \ "parent").lazyRead(Reads.optionWithNull(channelReads)) and
        (__ \ "children").lazyRead(Reads.seq[Channel](channelReads)) and
        (__ \ "hasChildren").read[Boolean] and
        (__ \ "lastModifiedDate").read[Long] and
        (__ \ "metadata").readNullable[ChannelMetadataNew]
      ) (Channel)
  }

  object PartialChannelReads {

    import SimpleFormats._

    implicit lazy val noChildrenReads: Reads[Channel] = new Reads[Channel] {
      override def reads(json: JsValue): JsResult[Channel] = json match {
        case JsObject(underlying) ⇒ (for {
          id ← underlying.get("id").map(_.as[ChannelId])
          data ← underlying.get("data").map(_.as[ChannelData])
        } yield JsSuccess(Channel(id = id, data = data, metadata = underlying.get("metadata").flatMap(_.asOpt[ChannelMetadataNew]))))
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

    implicit lazy val channelWrites: Writes[Channel] = (
      (__ \ "id").write[ChannelId] and
        (__ \ "data").write[ChannelData] and
        (__ \ "stages").writeNullable[Seq[Stage]] and
        (__ \ "parent").lazyWrite(Writes.optionWithNull(PartialChannelWrites.writeChannelAsNull)) and // avoid loops
        (__ \ "children").lazyWrite(Writes.seq[Channel](channelWrites)) and
        (__ \ "hasChildren").write[Boolean] and
        (__ \ "lastModifiedDate").write[Long] and
        (__ \ "metadata").writeNullable[ChannelMetadataNew]
      ) (unlift(Channel.unapply))

  }

  object PartialChannelWrites {

    import SimpleFormats._
    import StageFormats._

    implicit lazy val noChildrenWrites = new Writes[Channel] {
      override def writes(o: Channel): JsValue = JsObject(Map(
        "id" → Json.toJson(o.id),
        "lastModifiedDate" → JsNumber(o.lastModifiedDate),
        "hasChildren" → JsBoolean(o.hasChildren),
        "data" → Json.toJson(o.data)
      ) ++ o.stages.map { stage ⇒ "stages" → Json.toJson(stage) }
        ++ o.metadata.map { metadata ⇒ "metadata" → Json.toJson(metadata) }
      )
    }

    implicit lazy val oneLevelOfChildren: Writes[Channel] = (
      (__ \ "id").write[ChannelId] and
        (__ \ "data").write[ChannelData] and
        (__ \ "stages").writeNullable[Seq[Stage]] and
        (__ \ "parent").lazyWrite(Writes.optionWithNull(noChildrenWrites)) and
        (__ \ "children").lazyWrite(Writes.seq[Channel](noChildrenWrites)) and
        (__ \ "hasChildren").write[Boolean] and
        (__ \ "lastModifiedDate").write[Long] and
        (__ \ "metadata").writeNullable[ChannelMetadataNew]
      ) (unlift(Channel.unapply))

    implicit lazy val writeChannelAsNull: Writes[Channel] = new Writes[Channel] {
      override def writes(o: Channel): JsValue = JsNull
    }
  }

}

object ChannelFormatNoChildren {
  implicit lazy val channelFormat: Format[Channel] = Format(reads.PartialChannelReads.noChildrenReads, PartialChannelWrites.noChildrenWrites)
}

object SimpleFormats {
  implicit lazy val idFormat: Format[ChannelId] = Json.format[ChannelId]
  implicit lazy val adFormat: Format[ChannelAdData] = Json.format[ChannelAdData]
  implicit lazy val metaDataFormat: Format[ChannelMetadata] = Json.format[ChannelMetadata]
  implicit lazy val siteBuildingFormat: Format[ChannelSiteBuilding] = Json.format[ChannelSiteBuilding]
  implicit lazy val metaDataNewFormat: Format[ChannelMetadataNew] = Json.format[ChannelMetadataNew]
  implicit lazy val dataFormat: Format[ChannelData] = Json.format[ChannelData]

}


