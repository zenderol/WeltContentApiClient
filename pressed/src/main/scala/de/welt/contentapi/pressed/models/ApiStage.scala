package de.welt.contentapi.pressed.models

import de.welt.contentapi.core.models.ApiReference

/** Stage Model for Rendering in Funkotron
  * @param teasers All Teasers that belong to a Stage
  * @param configuration Configuration for that stage, with layout, label etc.
  */
case class ApiStage(teasers: Seq[ApiTeaser],
                    configuration: Option[ApiStageConfiguration] = None)

/**
  * @param layout "Default", "Hidden", "Carousel"
  * @param label Label to show above a stage
  * @param references References to render with href and label, e.g. Sub Ressorts
  * @param commercials contains the format ids for the Ads
  */
case class ApiStageConfiguration(layout: String = "Default",
                                 label: Option[String],
                                 references: Option[Seq[ApiReference]] = None,
                                 commercials: Option[Seq[String]] = None) {
  lazy val unwrappedCommercials: Seq[String] = commercials.getOrElse(Nil)
  lazy val unwrappedReferences: Seq[ApiReference] = references.getOrElse(Nil)
}

