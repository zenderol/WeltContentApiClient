package de.welt.contentapi.core.models

import play.api.libs.json.Json

object pressed {

  case class SectionPage(config: SectionPageConfig, stages: Seq[ContentStage] = Seq.empty)

  case class SectionPageConfig(label: String,
                               adZone: String,
                               path: String,
                               theme: SectionPageTheme)

  case class SectionPageTheme(name: String = "default")

  case class ContentStage(config: StageConfig, content: Seq[PressedContent])

  case class StageConfig(id: String= "default",
                         theme: StageMetadata = StageMetadata(),
                         headline: Option[String] = None,
                         path: Option[String] = None,
                         lazyLoaded: Boolean = false
                        )

  case class StageMetadata(name: String = "default", sectionReferences: Seq[SectionReference] = Nil)

  case class SectionReference(path: String, label: String)

  case class PressedContent(config: PressedContentConfig, content: EnrichedApiContent)

  case class PressedContentConfig(profile: String = "wide")


  object PressedFormats {
    import SectionDataFormats._
    import ApiFormats._
    // reads
    implicit lazy val SectionReferenceReads = Json.reads[SectionReference]

    // writes
    implicit lazy val PressedContentConfigFormat = Json.format[PressedContentConfig]
    implicit lazy val PressedContentFormat = Json.format[PressedContent]
    implicit lazy val SectionReferenceFormat = Json.format[SectionReference]
    implicit lazy val StageMetadataFormat = Json.format[StageMetadata]
    implicit lazy val StageConfigWrites = Json.format[StageConfig]
    implicit lazy val ContentStageWrites = Json.format[ContentStage]
    implicit lazy val SectionPageThemeWrites = Json.format[SectionPageTheme]
    implicit lazy val SectionPageConfigWrites = Json.format[SectionPageConfig]
    implicit lazy val SectionPageWrites = Json.format[SectionPage]
  }

}