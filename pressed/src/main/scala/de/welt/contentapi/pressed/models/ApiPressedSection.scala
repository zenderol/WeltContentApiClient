package de.welt.contentapi.pressed.models

/**
  * A complete section page with all teasers and page configuration.
  *
  * @param stages        all stages of the section. Includes (pressed) content teasers and the config.
  * @param channel       channel with breadcrumb of the section. (ConfigMcConfigFace)
  * @param configuration configuration for the section page. (ConfigMcConfigFace)
  */
case class ApiPressedSection(stages: Seq[ApiStage] = Nil,
                             channel: Option[ApiChannel] = None,
                             configuration: Option[ApiConfiguration] = None)
