package de.welt.contentapi.core.models

import de.welt.contentapi.core.models.reads.FullChannelReads
import de.welt.contentapi.core.models.writes.FullChannelWrites

object pressed {

  case class ApiSectionPage(config: SectionPageConfig, stages: Seq[ContentStage] = Seq.empty)

  case class SectionPageConfig(displayName: String,
                               pathForAdvertisement: String,
                               path: String,
                               theme: ChannelTheme = ChannelTheme.DEFAULT,
                               eceId: Long,
                               fields: Option[Map[String, String]],
                               breadCrumb: Seq[Channel] = Nil
                              )

  object SectionPageConfig {
    def fromChannel(channel: Channel) = SectionPageConfig(
      displayName = channel.data.label,
      pathForAdvertisement = channel.getAdTag.getOrElse(channel.DEFAULT_AD_TAG),
      path = channel.id.path,
      theme = channel.data.siteBuilding.getOrElse(ChannelTheme.DEFAULT),
      eceId = channel.id.ece,
      fields = channel.data.fields,
      breadCrumb = channel.getBreadcrumb()
    )
  }

  case class ContentStage(index: Int,
                          id: String,
                          config: StageConfig,
                          content: Seq[PressedContent])


  case class PressedContent(config: PressedContentConfig, content: EnrichedApiContent)

  /**
    * Defines how a single Content should be displayed
    * @param profile The view profile, one of ```[tiny, small, half, medium, wide]```
    * @param `type` Specifies the teaser Type, one of ```[Hero, Mediahero, Default, DefaultIcon, Simple, Upright, Inline, Cluster, Counter, Newsticker]```
    */
  case class PressedContentConfig(profile: String = "wide", `type`: String = "Default")


  object PressedFormats {

    import play.api.libs.json._
    import SectionDataFormats._
    import ApiFormats._
    import StageFormats.stageConfigFormat
    import SimpleFormats.channelThemeFormat
    import ChannelFormatNoChildren.channelFormat

    implicit val channelFormatFullChildren: Format[Channel] = Format(FullChannelReads.channelReads, FullChannelWrites.channelWrites)

    // formats
    implicit lazy val pressedContentConfigFormat: Format[PressedContentConfig] = Json.format[PressedContentConfig]
    implicit lazy val pressedContentFormat: Format[PressedContent] = Json.format[PressedContent]
    implicit lazy val contentStageFormat: Format[ContentStage] = Json.format[ContentStage]
    implicit lazy val sectionPageConfigFormat: Format[SectionPageConfig] = Json.format[SectionPageConfig]
    implicit lazy val sectionPageFormat: Format[ApiSectionPage] = Json.format[ApiSectionPage]
  }

}