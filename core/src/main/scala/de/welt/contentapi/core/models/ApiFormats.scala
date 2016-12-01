package de.welt.contentapi.core.models

import play.api.libs.json._

/**
  * Import these into your scope to easily transform Json to the required object.
  * INFO: the order of the reads is very important.
  * see: http://stackoverflow.com/q/26086815/
  */
object ApiFormats {
  implicit lazy val apiReferenceFormat: Format[ApiReference] = Json.format[ApiReference]
  implicit lazy val apiOnwardFormat: Format[ApiOnward] = Json.format[ApiOnward]
  implicit lazy val apiMetadataFormat: Format[ApiMetadata] = Json.format[ApiMetadata]
  implicit lazy val apiAssetFormat: Format[ApiAsset] = Json.format[ApiAsset]
  implicit lazy val apiSectionDataFormat: Format[ApiSectionData] = Json.format[ApiSectionData]
  implicit lazy val apiElementFormat: Format[ApiElement] = Json.format[ApiElement]
  implicit lazy val apiAuthorFormat: Format[ApiAuthor] = Json.format[ApiAuthor]
  implicit lazy val apiTagFormat: Format[ApiTag] = Json.format[ApiTag]
  implicit lazy val apiContentFormat: Format[ApiContent] = Json.format[ApiContent]
  implicit lazy val apiResponseFormat: Format[ApiResponse] = Json.format[ApiResponse]
}


object ApiReads {
  implicit lazy val apiReferenceReads: Reads[ApiReference] = Json.reads[ApiReference]
  implicit lazy val apiOnwardReads: Reads[ApiOnward] = Json.reads[ApiOnward]
  implicit lazy val apiMetadataReads: Reads[ApiMetadata] = Json.reads[ApiMetadata]
  implicit lazy val apiSectionDataReads: Reads[ApiSectionData] = Json.reads[ApiSectionData]
  implicit lazy val apiAssetReads: Reads[ApiAsset] = Json.reads[ApiAsset]
  implicit lazy val apiElementReads: Reads[ApiElement] = Json.reads[ApiElement]
  implicit lazy val apiAuthorReads: Reads[ApiAuthor] = Json.reads[ApiAuthor]
  implicit lazy val apiTagReads: Reads[ApiTag] = Json.reads[ApiTag]
  implicit lazy val apiContentReads: Reads[ApiContent] = Json.reads[ApiContent]
  implicit lazy val apiResponseReads: Reads[ApiResponse] = Json.reads[ApiResponse]
}

object ApiWrites {
  implicit lazy val apiSectionReferenceWrites: Writes[ApiReference] = Json.writes[ApiReference]
  implicit lazy val apiOnwardWrites: Writes[ApiOnward] = Json.writes[ApiOnward]
  implicit lazy val apiMetadataWrites: Writes[ApiMetadata] = Json.writes[ApiMetadata]
  implicit lazy val apiAssetWrites: Writes[ApiAsset] = Json.writes[ApiAsset]
  implicit lazy val apiSectionDataWrites: Writes[ApiSectionData] = Json.writes[ApiSectionData]
  implicit lazy val apiElementWrites: Writes[ApiElement] = Json.writes[ApiElement]
  implicit lazy val apiAuthorWrites: Writes[ApiAuthor] = Json.writes[ApiAuthor]
  implicit lazy val apiTagWrites: Writes[ApiTag] = Json.writes[ApiTag]
  implicit lazy val apiContentWrites: Writes[ApiContent] = Json.writes[ApiContent]
  implicit lazy val apiResponseWrites: Writes[ApiResponse] = Json.writes[ApiResponse]
}


