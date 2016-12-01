package de.welt.contentapi.legacy.models

import de.welt.contentapi.core.models.ApiContent
import de.welt.contentapi.pressed.models.{ApiChannel, ApiConfiguration, ApiPressedContent}

/**
  * da-hood Model: ApiLists
  */
case class ApiLegacySection(lists: Option[Seq[ApiLegacyStage]] = None) {
  lazy val unwrappedLists: Seq[ApiLegacyStage] = lists.getOrElse(Nil)
}

/**
  * da-hood Model: ApiStages
  */
case class ApiLegacyStage(id: String,
                          label: Option[String] = None,
                          content: Option[Seq[ApiContent]] = None) {
  lazy val unwrappedContent: Seq[ApiContent] = content.getOrElse(Nil)
}

/**
  * New wrapper Model: da-hood + CMCF
  */
case class ApiLegacyPressedSection(stages: Option[Seq[ApiLegacyPressedStage]] = None,
                                   channel: Option[ApiChannel] = None,
                                   configuration: Option[ApiConfiguration] = None) {
  lazy val unwrappedStages: Seq[ApiLegacyPressedStage] = stages.getOrElse(Nil)
}

/**
  * New wrapper Model: da-hood + CMCF
  */
case class ApiLegacyPressedStage(id: String,
                                 label: Option[String] = None,
                                 content: Option[Seq[ApiPressedContent]] = None) {
  lazy val unwrappedContent: Seq[ApiPressedContent] = content.getOrElse(Nil)
}
