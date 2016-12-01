package de.welt.contentapi.raw.client.models

import java.time.Instant

import de.welt.contentapi.raw.models.{RawChannel, RawChannelConfiguration, RawChannelId, RawMetadata}

case class SdpSectionData(url: String,
                          displayName: String,
                          lastModifiedDate: Option[String],
                          children: Seq[SdpSectionData],
                          id: Long) {

  private def defineAdTags(data: RawChannelConfiguration) = data.copy(commercial = data.commercial.copy(definesAdTag = true, definesVideoAdTag = true))

  def toChannel: RawChannel = {
    val root = transform
    // initially set hasAdTag to true for level 0 & 1 of the section tree
    root.config = defineAdTags(root.config)
    root.children.foreach { child ⇒ child.config = defineAdTags(child.config) }

    // set the parent-relation for all elements
    root.updateParentRelations()
    root
  }

  private def transform: RawChannel = RawChannel(
    id = RawChannelId(path = url, escenicId = id, label = displayName),
    children = children.map(_.transform),
    metadata = RawMetadata(lastModifiedDate = lastModifiedDate match {
      case Some(s) if s.nonEmpty ⇒ s.toLong
      case _ ⇒ Instant.now.toEpochMilli
    })
  )
}

object SdpSectionDataReads {

  import play.api.libs.functional.syntax._
  import play.api.libs.json.Reads._
  import play.api.libs.json._

  implicit lazy val s3SectionDataReads: Reads[SdpSectionData] = (
    (JsPath \ "url").read[String] and
      (JsPath \ "displayName").read[String] and
      (JsPath \ "lastModifiedDate").readNullable[String] and
      (JsPath \ "children").lazyRead(Reads.seq[SdpSectionData](s3SectionDataReads)) and
      (JsPath \ "id").read[String].map(_.toLong)
    ) (SdpSectionData)
}
