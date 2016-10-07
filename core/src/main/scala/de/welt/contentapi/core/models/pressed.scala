package de.welt.contentapi.core.models

import de.welt.contentapi.core.models.reads.FullChannelReads
import de.welt.contentapi.core.models.writes.FullChannelWrites

object pressed {
  
  /**
    * @param channelConfig channel configuration for the section [from@ConfigMcConfigFace]
    * @param stages all stages of the section
    */
  case class ApiSectionPage(channelConfig: Option[ApiChannelConfig] = None, stages: Option[Seq[ApiContentStage]] = None) {
    lazy val unwrappedStages: Seq[ApiContentStage] = stages.getOrElse(Nil)
  }
  
  /**
    * Short version of (root) ApiChannel just for a section page
    *
    * @param id channel id with section path
    * @param data configured section data [from@ConfigMcConfigFace]
    * @param metadata some meta data (changed by)
    * @param lastModifiedDate last mod date
    * @param escenicId Escenic (CMS) internal section id
    * @param breadcrumb breadcrumb for the section
    */
  case class ApiChannelConfig(id: Option[ChannelId] = None,
                              data: Option[ApiChannelData] = None,
                              metadata: Option[ApiChannelMetadataNew] = None,
                              lastModifiedDate: Option[Long] = None,
                              pathForAdvertisement: Option[String] = None,
                              escenicId: Option[String] = None,
                              breadcrumb: Option[Seq[ApiChannel]] = None) {
    lazy val unwrappedBreadcrumb: Seq[ApiChannel] = breadcrumb.getOrElse(Nil)
  }

  object ApiChannelConfig {
    def fromApiChannel(apiChannel: ApiChannel): ApiChannelConfig = new ApiChannelConfig(
      id = Some(apiChannel.id),
      data = Some(apiChannel.data),
      metadata = apiChannel.metadata,
      lastModifiedDate = Some(apiChannel.lastModifiedDate),
      pathForAdvertisement = apiChannel.getAdTag.orElse(Some(apiChannel.DEFAULT_AD_TAG)),
      escenicId = Some(apiChannel.id.ece.toString),
      breadcrumb = Some(apiChannel.getBreadcrumb())
    )
  }

  /**
    * @param index stage index -- starts at 0
    * @param id ???
    * @param config configuration of the stage (headline, theming, commercials) [from@ConfigMcConfigFace]
    * @param content all content (articles) of the stage. rendered as teasers.
    */
  case class ApiContentStage(index: Int = -1,
                             id: Option[String] = None,
                             config: Option[ApiStageConfig] = None,
                             content: Option[Seq[ApiPressedContent]] = None) {
    lazy val unwrappedContent: Seq[ApiPressedContent] = content.getOrElse(Nil)
  }

  object ApiContentStage {
    def fromStageConfigAndContent(stage: Stage, content: Seq[ApiPressedContent]): ApiContentStage = ApiContentStage(
      index = stage.index,
      id = Some(stage.id),
      config = Some(new ApiStageConfig(
        stageType = stage.config.flatMap(_.stageType),
        stageTheme = stage.config.flatMap(_.stageTheme),
        headlineTheme = stage.config.flatMap(_.headlineTheme),
        sectionReferences = stage.config.flatMap(_.sectionReferences),
        commercials = stage.config.flatMap(_.commercials)
      )),
      content = Some(content)
    )
  }
  
  /**
    * @param stageType rendering option for a stage (default, carousel, user-defined, hidden) [mapping@funkotron]
    * @param stageTheme theming options of the stage [mapping@funkotron]
    * @param headlineTheme theming options for the stage headline
    * @param sectionReferences links to other sections (e.g. Sendungen A-Z)
    * @param commercials available commercials for a stage. positioning is made by funkotron
    */
  case class ApiStageConfig(stageType: Option[String] = None,
                            stageTheme: Option[ApiStageTheme] = None,
                            headlineTheme: Option[ApiHeadlineTheme] = None,
                            sectionReferences: Option[Seq[ApiSectionReference]] = None,
                            commercials: Option[Seq[ApiCommercial]] = None) {
    lazy val unwrappedSectionReferences: Seq[ApiSectionReference] = sectionReferences.getOrElse(Nil)
    lazy val unwrappedCommercials: Seq[ApiCommercial] = commercials.getOrElse(Nil)
  }

  object ApiStageConfig {
    def fromStage(stage: Stage): ApiStageConfig = new ApiStageConfig(
      stageType = stage.config.flatMap(_.stageType),
      stageTheme = stage.config.flatMap(_.stageTheme),
      headlineTheme = stage.config.flatMap(_.headlineTheme),
      sectionReferences = stage.config.flatMap(_.sectionReferences),
      commercials = stage.config.flatMap(_.commercials)
    )
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
    import SimpleFormats._
    import ChannelFormatNoChildren.channelFormat

    implicit val channelFormatFullChildren: Format[ApiChannel] = Format(FullChannelReads.channelReads, FullChannelWrites.channelWrites)

    // formats
    implicit lazy val apiPressedContentTeaserConfigFormat: Format[ApiPressedContentTeaserConfig] = Json.format[ApiPressedContentTeaserConfig]
    implicit lazy val apiPressedContentFormat: Format[ApiPressedContent] = Json.format[ApiPressedContent]
    implicit lazy val apiStageConfigFormat: Format[ApiStageConfig] = Json.format[ApiStageConfig]
    implicit lazy val apiContentStageFormat: Format[ApiContentStage] = Json.format[ApiContentStage]
    implicit lazy val apiChannelConfigFormat: Format[ApiChannelConfig] = Json.format[ApiChannelConfig]
    implicit lazy val apiSectionPageFormat: Format[ApiSectionPage] = Json.format[ApiSectionPage]
  }

}
