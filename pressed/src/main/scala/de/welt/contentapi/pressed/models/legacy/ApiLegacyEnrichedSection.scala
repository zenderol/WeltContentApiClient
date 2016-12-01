package de.welt.contentapi.pressed.models.legacy

import de.welt.contentapi.pressed.models.{ApiChannel, ApiConfiguration}

/**
  * Legacy == papyrus -> da-hood version of sections
  *
  * @param section section with stages with teasers (Da-hood)
  * @param channel channel with breadcrumb (ConfigMcConfigFace)
  * @param configuration configuration for the section page (ConfigMcConfigFace)
  */
case class ApiLegacyEnrichedSection(section: Option[ApiLegacySection] = None,
                                    channel: Option[ApiChannel] = None,
                                    configuration: Option[ApiConfiguration] = None)
