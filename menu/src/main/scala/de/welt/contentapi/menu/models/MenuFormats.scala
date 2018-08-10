package de.welt.contentapi.menu.models

import play.api.libs.json._

/**
  * Import these into your scope to easily transform Json to the required object.
  * INFO: the order of the reads is very important.
  */
object MenuFormats {

  import MenuJsonReads._
  import MenuJsonWrites._

  implicit lazy val MenuReferenceFormat: Format[MenuReference] = Format(MenuReferenceReads, MenuReferenceWrites)
  implicit lazy val MenuMetadataFormat: Format[MenuMetadata] = Format(MenuMetadataReads, MenuMetadataWrites)
  implicit lazy val MenuLinkFormat: Format[MenuLink] = Format(MenuLinkReads, MenuLinkWrites)
  implicit lazy val MenuFormat: Format[Menu] = Format(MenuReads, MenuWrites)
}

object MenuJsonReads {
  implicit lazy val MenuReferenceReads: Reads[MenuReference] = Json.reads[MenuReference]
  implicit lazy val MenuMetadataReads: Reads[MenuMetadata] = Json.reads[MenuMetadata]
  implicit lazy val MenuLinkReads: Reads[MenuLink] = Json.reads[MenuLink]
  implicit lazy val MenuReads: Reads[Menu] = Json.reads[Menu]
}

object MenuJsonWrites {
  implicit lazy val MenuReferenceWrites: Writes[MenuReference] = Json.writes[MenuReference]
  implicit lazy val MenuMetadataWrites: Writes[MenuMetadata] = Json.writes[MenuMetadata]
  implicit lazy val MenuLinkWrites: Writes[MenuLink] = Json.writes[MenuLink]
  implicit lazy val MenuWrites: Writes[Menu] = Json.writes[Menu]
}


