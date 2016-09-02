package de.welt.contentapi.core.models

import de.welt.contentapi.core.models.reads.FullChannelReads
import de.welt.contentapi.core.models.writes.FullChannelWrites

object pressed {

  case class SectionPage(config: SectionPageConfig, stages: Seq[ContentStage] = Seq.empty)

  case class SectionPageConfig(displayName: String,
                               pathForAdtech: String,
                               path: String,
                               theme: SectionPageTheme,
                               eceId: Long,
                               fields: Option[Map[String, String]]
                              )

  object SectionPageConfig {
    def fromChannel(channel: Channel) = SectionPageConfig(
      displayName = channel.data.label,
      pathForAdtech = channel.getAdTag.getOrElse("sonstiges"),
      path = channel.id.path,
      theme = SectionPageTheme(),
      eceId = channel.id.ece,
      fields = channel.data.fields
    )
  }

  case class SectionPageTheme(name: String = "default",
                              bgColor: Option[String] = None
                             )

  case class ContentStage(index: Int, config: StageConfig, content: Seq[PressedContent])

  case class StageConfig(id: String = "default",
                         theme: Option[StageTheme] = None,
                         headlineTheme: Option[HeadlineTheme] = None,
                         path: Option[String] = None,
                         sectionReferences: Seq[SectionReference] = Nil,
                         lazyLoaded: Boolean = false,
                         commercial: Option[String] = None
                        )

  case class HeadlineTheme(headline: String,
                           size: Option[String],
                           weight: Option[String]
                           )

  case class StageTheme(name: String = "default",
                        bgColor: Option[String] = None,
                        itemGap: Option[String] = None,
                        stageGap: Option[String] = None,
                        isHidden: Boolean = false,
                        isFrameless: Boolean = true)

  case class SectionReference(path: String, label: String)

  case class PressedContent(config: PressedContentConfig, content: EnrichedApiContent)

  /**
    * Defines how a single Content should be displayed
    * @param profile The view profile, one of ```[tiny, small, half, medium, wide]```
    * @param `type` Specifies the teaser Type, one of ```[Hero, Mediahero, Default, DefaultIcon, Simple, Upright, Inline, Cluster, Counter, Newsticker]```
    */
  case class PressedContentConfig(profile: String = "wide", `type`: String = "Default")


  object PressedFormats {

    import play.api.libs.json._
    import de.welt.contentapi.core.models.SectionDataFormats._
    import de.welt.contentapi.core.models.ApiFormats._
    implicit val channelFormatFullChildren: Format[Channel] = Format(FullChannelReads.channelReads, FullChannelWrites.channelWrites)

    // formats
    implicit lazy val pressedContentConfigFormat: Format[PressedContentConfig] = Json.format[PressedContentConfig]
    implicit lazy val pressedContentFormat: Format[PressedContent] = Json.format[PressedContent]
    implicit lazy val sectionReferenceFormat: Format[SectionReference] = Json.format[SectionReference]
    implicit lazy val headlineThemeFormat: Format[HeadlineTheme] = Json.format[HeadlineTheme]
    implicit lazy val stageThemeFormat: Format[StageTheme] = Json.format[StageTheme]
    implicit lazy val stageConfigFormat: Format[StageConfig] = Json.format[StageConfig]
    implicit lazy val contentStageFormat: Format[ContentStage] = Json.format[ContentStage]
    implicit lazy val sectionPageThemeFormat: Format[SectionPageTheme] = Json.format[SectionPageTheme]
    implicit lazy val sectionPageConfigFormat: Format[SectionPageConfig] = Json.format[SectionPageConfig]
    implicit lazy val sectionPageFormat: Format[SectionPage] = Json.format[SectionPage]
  }

}