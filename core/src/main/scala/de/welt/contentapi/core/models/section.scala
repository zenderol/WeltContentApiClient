package de.welt.contentapi.core.models

import de.welt.contentapi.core.models.reads.FullChannelReads
import de.welt.contentapi.core.models.writes.FullChannelWrites

case class EnrichedApiContent(content: ApiContent, sectionData: Option[SectionData]) {
  def `type`: String = content.`type`
}

case class EnrichedApiResponse(main: EnrichedApiContent, related: List[EnrichedApiContent] = Nil) {

  def onward = related.filter(_.content.unwrappedRoles.contains("onward"))

  def playlist = related.filter(_.content.unwrappedRoles.contains("playlist"))
}

case class SectionData(home: Channel, breadcrumb: Seq[Channel])

object SectionData {
  def fromChannel(channel: Channel) = {
    SectionData(channel.copy(hasChildren = false, children = Seq.empty), channel.getBreadcrumb())
  }
}


object SectionDataFormats {
  import play.api.libs.json._
  import de.welt.contentapi.core.models.ApiFormats._
  implicit val channelFormatFullChildren: Format[Channel] = Format(FullChannelReads.channelReads, FullChannelWrites.channelWrites)

  implicit lazy val sectionDataFormat: Format[SectionData] = Json.format[SectionData]
  implicit lazy val EnrichedApiContentFormat: Format[EnrichedApiContent] = Json.format[EnrichedApiContent]
}
