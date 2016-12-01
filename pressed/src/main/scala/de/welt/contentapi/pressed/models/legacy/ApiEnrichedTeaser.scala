package de.welt.contentapi.pressed.models.legacy

import de.welt.contentapi.core.models.ApiContent
import de.welt.contentapi.pressed.models.ApiChannel

case class ApiEnrichedTeaser(content: Option[ApiContent] = None,
                             channel: Option[ApiChannel] = None)
