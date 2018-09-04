package de.welt.contentapi.menu.models

import play.api.libs.json._

/**
  * Import these into your scope to easily transform Json to the required object.
  * INFO: the order of the reads is very important.
  */
object MenuFormats {

  import de.welt.contentapi.core.models.ApiFormats.apiReferenceFormat
  import MenuJsonReads._
  import MenuJsonWrites._

  implicit lazy val apiMenuMetadataFormat: Format[ApiMenuMetadata] = Format(apiMenuMetadataReads, apiMenuMetadataWrites)
  implicit lazy val apiMenuLinkFormat: Format[ApiMenuLink] = Format(apiMenuLinkReads, apiMenuLinkWrites)
  implicit lazy val apiMenuFormat: Format[ApiMenu] = Format(apiMenuReads, apiMenuWrites)
}

object MenuJsonReads {
  import de.welt.contentapi.core.models.ApiReads.apiReferenceReads

  implicit lazy val apiMenuMetadataReads: Reads[ApiMenuMetadata] = Json.reads[ApiMenuMetadata]
  implicit lazy val apiMenuLinkReads: Reads[ApiMenuLink] = Json.reads[ApiMenuLink]
  implicit lazy val apiMenuReads: Reads[ApiMenu] = Json.reads[ApiMenu]
}

object MenuJsonWrites {
  import de.welt.contentapi.core.models.ApiWrites.apiReferenceWrites

  implicit lazy val apiMenuMetadataWrites: Writes[ApiMenuMetadata] = Json.writes[ApiMenuMetadata]
  implicit lazy val apiMenuLinkWrites: Writes[ApiMenuLink] = Json.writes[ApiMenuLink]
  implicit lazy val apiMenuWrites: Writes[ApiMenu] = Json.writes[ApiMenu]
}


