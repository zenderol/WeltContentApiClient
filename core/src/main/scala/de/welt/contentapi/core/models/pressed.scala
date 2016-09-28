package de.welt.contentapi.core.models

import de.welt.contentapi.core.models.reads.FullChannelReads
import de.welt.contentapi.core.models.writes.FullChannelWrites

object pressed {

  case class ApiSectionPage(config: ApiSectionPageConfig, stages: Seq[ApiContentStage] = Seq.empty)

  case class ApiSectionPageConfig(displayName: String,
                                  pathForAdvertisement: String,
                                  path: String,
                                  theme: ApiChannelTheme = ApiChannelTheme.DEFAULT,
                                  eceId: Long,
                                  fields: Option[Map[String, String]],
                                  breadcrumb: Seq[ApiChannel] = Nil
                              )

  object ApiSectionPageConfig {
    def fromChannel(channel: ApiChannel) = ApiSectionPageConfig(
      displayName = channel.data.label,
      pathForAdvertisement = channel.getAdTag.getOrElse(channel.DEFAULT_AD_TAG),
      path = channel.id.path,
      theme = channel.data.siteBuilding.getOrElse(ApiChannelTheme.DEFAULT),
      eceId = channel.id.ece,
      fields = channel.data.fields,
      breadcrumb = channel.getBreadcrumb()
    )
  }

  case class ApiContentStage(index: Int,
                             id: String,
                             config: ApiStageConfig,
                             content: Seq[ApiPressedContent])

  object ApiContentStage {
    def fromStageConfigAndContent(stage: Stage, content: Seq[ApiPressedContent]) = {
      ApiContentStage(
        index = stage.index,
        id = stage.id,
        config = new ApiStageConfig(
          stageType = stage.config.flatMap(_.stageType),
          stageTheme = stage.config.flatMap(_.stageTheme),
          headlineTheme = stage.config.flatMap(_.headlineTheme),
          sectionReferences = stage.config.map(_.sectionReferences),
          commercials = stage.config.flatMap(_.commercials)
        ),
        content = content
      )
    }
  }

  case class ApiStageConfig(stageType: Option[String] = None,
                            stageTheme: Option[StageTheme] = None,
                            headlineTheme: Option[HeadlineTheme] = None,
                            sectionReferences: Option[Seq[SectionReference]] = None,
                            commercials: Option[Seq[Commercial]] = None)

  object ApiStageConfig {
    def fromStage(stage: Stage): Unit = {
      new ApiStageConfig(
        stageType = stage.config.flatMap(_.stageType),
        stageTheme = stage.config.flatMap(_.stageTheme),
        headlineTheme = stage.config.flatMap(_.headlineTheme),
        sectionReferences = stage.config.map(_.sectionReferences),
        commercials = stage.config.flatMap(_.commercials)
      )
    }
  }

  case class ApiPressedContent(teaserConfig: ApiPressedContentTeaserConfig,
                               content: EnrichedApiContent)

  /**
    * Defines how a single Content should be displayed
    * @param profile The view profile, one of ```[tiny, small, half, medium, wide]```
    * @param `type` Specifies the teaser Type, one of ```[Hero, Mediahero, Default, DefaultIcon, Simple, Upright, Inline, Cluster, Counter, Newsticker]```
    */
  case class ApiPressedContentTeaserConfig(profile: String = "wide", `type`: String = "Default")


  object PressedFormats {

    import play.api.libs.json._
    import SectionDataFormats._
    import ApiFormats._
    import StageFormats._
    import SimpleFormats.channelThemeFormat
    import ChannelFormatNoChildren.channelFormat

    implicit val channelFormatFullChildren: Format[ApiChannel] = Format(FullChannelReads.channelReads, FullChannelWrites.channelWrites)

    // formats
    implicit lazy val pressedContentConfigFormat: Format[ApiPressedContentTeaserConfig] = Json.format[ApiPressedContentTeaserConfig]
    implicit lazy val pressedContentFormat: Format[ApiPressedContent] = Json.format[ApiPressedContent]
    implicit lazy val apiStageConfigFormat: Format[ApiStageConfig] = Json.format[ApiStageConfig]
    implicit lazy val contentStageFormat: Format[ApiContentStage] = Json.format[ApiContentStage]
    implicit lazy val sectionPageConfigFormat: Format[ApiSectionPageConfig] = Json.format[ApiSectionPageConfig]
    implicit lazy val sectionPageFormat: Format[ApiSectionPage] = Json.format[ApiSectionPage]
  }

}