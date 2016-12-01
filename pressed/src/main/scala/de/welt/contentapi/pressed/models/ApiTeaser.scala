package de.welt.contentapi.pressed.models


case class ApiTeaser(teaserConfig: ApiTeaserConfig, data: ApiPressedContent)

case class ApiTeaserConfig(profile: String, `type`: String)
