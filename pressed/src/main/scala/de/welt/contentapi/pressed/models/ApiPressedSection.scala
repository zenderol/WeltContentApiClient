package de.welt.contentapi.pressed.models

/**
  * @param stages        todo harry: add docs!
  * @param channel       channel with breadcrumb of the section. (ConfigMcConfigFace)
  * @param configuration configuration for the section page. (ConfigMcConfigFace)
  */
case class ApiPressedSection(stages: Seq[ApiStage] = Nil,
                             channel: Option[ApiChannel] = None,
                             configuration: Option[ApiConfiguration] = None)
