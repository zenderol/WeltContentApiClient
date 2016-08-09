package de.welt.contentapi.core.models

import de.welt.contentapi.core.models.pressed.SectionReference
import play.api.libs.json.Json

case class EnrichedApiContent(content: ApiContent, sectionData: Option[SectionData]) {
  def `type`: String = content.`type`
}

case class EnrichedApiResponse(main: EnrichedApiContent, related: List[EnrichedApiContent] = Nil) {

  def onward = related.filter(_.content.unwrappedRoles.contains("onward"))

  def playlist = related.filter(_.content.unwrappedRoles.contains("playlist"))
}

case class SectionData(home: Channel, breadcrumb: Seq[Channel])

object SectionData {
  def apply(path: String, label: String, breadcrumb: Seq[Channel], definesAdTag: Boolean = false): SectionData = {
    val channelData: ChannelData = ChannelData(label, adData = ChannelAdData(definesAdTag = definesAdTag))

    SectionData(Channel(ChannelId(path), channelData), breadcrumb)
  }
}

object SectionDataFormats {
  import ApiFormats._
  import ChannelFormatNoChildren.channelFormat

  implicit lazy val sectionDataWrites = Json.format[SectionData]
  implicit lazy val EnrichedApiContentWrites = Json.format[EnrichedApiContent]
}
