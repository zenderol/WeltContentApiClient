package de.welt.contentapi.legacy.models

import play.api.libs.json.{Format, Json, Reads, Writes}

object LegacyFormats {
  import de.welt.contentapi.core.models.ApiFormats._
  import de.welt.contentapi.pressed.models.PressedFormats._

  implicit val legacyPressedStageFormat: Format[ApiLegacyPressedStage] = Json.format[ApiLegacyPressedStage]
  implicit val legacyPressedSectionFormat: Format[ApiLegacyPressedSection] = Json.format[ApiLegacyPressedSection]

  implicit val legacyStageFormat: Format[ApiLegacyStage] = Json.format[ApiLegacyStage]
  implicit val legacySectionFormat: Format[ApiLegacySection] = Json.format[ApiLegacySection]
}

object LegacyReads {
  import de.welt.contentapi.core.models.ApiReads._
  import de.welt.contentapi.pressed.models.PressedReads._

  implicit val legacyPressedStageReads: Reads[ApiLegacyPressedStage] = Json.reads[ApiLegacyPressedStage]
  implicit val legacyPressedSectionReads: Reads[ApiLegacyPressedSection] = Json.reads[ApiLegacyPressedSection]

  implicit val legacyStageReads: Reads[ApiLegacyStage] = Json.reads[ApiLegacyStage]
  implicit val legacySectionReads: Reads[ApiLegacySection] = Json.reads[ApiLegacySection]
}

object LegacyWrites {
  import de.welt.contentapi.core.models.ApiWrites._
  import de.welt.contentapi.pressed.models.PressedWrites._

  implicit lazy val legacyPressedStageWrites: Writes[ApiLegacyPressedStage] = Json.writes[ApiLegacyPressedStage]
  implicit lazy val legacyPressedSectionWrites: Writes[ApiLegacyPressedSection] = Json.writes[ApiLegacyPressedSection]

  implicit lazy val legacyStageWrites: Writes[ApiLegacyStage] = Json.writes[ApiLegacyStage]
  implicit lazy val legacySectionWrites: Writes[ApiLegacySection] = Json.writes[ApiLegacySection]
}
