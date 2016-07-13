package models

import de.welt.contentapi.core.models.EnrichedApiContent


object pressed {

  case class SectionPage(config: SectionPageConfig, stages: Seq[ContentStage] = Seq.empty)

  case class SectionPageConfig(label: String,
                               adZone: String,
                               path: String,
                               theme: SectionPageTheme)

  case class SectionPageTheme(name: String = "default")

  case class ContentStage(config: StageConfig, content: Seq[PressedContent])

  case class StageConfig(theme: StageMetadata = StageMetadata(),
                         headline: Option[String] = None,
                         path: Option[String] = None,
                         lazyLoaded: Boolean = false
                        )

  case class StageMetadata(name: String = "default", sectionReferences: Seq[SectionReference] = Nil)

  case class SectionReference(path: String, label: String)

  case class PressedContent(config: PressedContentConfig, content: EnrichedApiContent)

  case class PressedContentConfig(profile: String = "wide")

}