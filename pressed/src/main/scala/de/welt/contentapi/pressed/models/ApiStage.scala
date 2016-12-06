package de.welt.contentapi.pressed.models

import de.welt.contentapi.core.models.ApiReference

/**
  * @param teasers       All Teasers that belong to a Stage
  * @param configuration Configuration for that Stage, with layout, label etc.
  */
case class ApiStage(teasers: Seq[ApiTeaser],
                    configuration: Option[ApiStageConfiguration] = None)

/**
  * @param layout      Name of the layout for the stage, e.g. 'channel-hero', 'multimedia' or 'hidden'
  * @param label       Label to show above a stage
  * @param references  References to render with href and label, e.g. Sub Ressorts
  * @param commercials contains the format ids for the Ads
  */
case class ApiStageConfiguration(layout: String = "Default",
                                 label: Option[String],
                                 references: Option[Seq[ApiReference]] = None,
                                 commercials: Option[Seq[String]] = None) {
  lazy val unwrappedCommercials: Seq[String] = commercials.getOrElse(Nil)
  lazy val unwrappedReferences: Seq[ApiReference] = references.getOrElse(Nil)
}

/**
  * Wraps the Teaser[[ApiPressedContent]] with it's Layout information.
  *
  * @param teaserConfig How to render a Teaser[[ApiPressedContent]] on a Section Page.
  * @param data         The Teaser content fully pressed with all it's data.
  */
case class ApiTeaser(teaserConfig: ApiTeaserConfig, data: ApiPressedContent)

/**
  * Describes how to render the Teaser[[ApiPressedContent]].
  *
  * @param profile Describes how the Teaser is rendered in a RWD grid. Think of: small, medium, large. This is only
  *                a mapping value for the client.
  * @param `type`  Teaser Type is the name/type of the Teaser -- e.g. 'DefaultTeaser' or 'HeroTeaser'. This is only
  *                a mapping value for the client.
  */
case class ApiTeaserConfig(profile: String, `type`: String)
