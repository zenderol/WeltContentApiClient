package de.welt.contentapi.core.models

import play.api.libs.json._

/**
  * Import these into your scope to easily transform Json to the required object.
  * INFO: the order of the reads is very important.
  * see: http://stackoverflow.com/q/26086815/
  */
object ApiFormats {
  import ApiReads._
  import ApiWrites._

  implicit lazy val apiReferenceFormat: Format[ApiReference] = Format(apiReferenceReads, apiReferenceWrites)
  implicit lazy val apiOnwardFormat: Format[ApiOnward] = Format(apiOnwardReads, apiOnwardWrites)
  implicit lazy val apiMetadataFormat: Format[ApiMetadata] = Format(apiMetadataReads, apiMetadataWrites)
  implicit lazy val apiAssetFormat: Format[ApiAsset] = Format(apiAssetReads, apiAssetWrites)
  implicit lazy val apiSectionDataFormat: Format[ApiSectionData] = Format(apiSectionDataReads, apiSectionDataWrites)
  implicit lazy val apiElementFormat: Format[ApiElement] = Format(apiElementReads, apiElementWrites)
  implicit lazy val apiAuthorFormat: Format[ApiAuthor] = Format(apiAuthorReads, apiAuthorWrites)
  implicit lazy val apiTagFormat: Format[ApiTag] = Format(apiTagReads, apiTagWrites)
  implicit lazy val apiContentFormat: Format[ApiContent] = Format(apiContentReads, apiContentWrites)
  implicit lazy val apiResponseFormat: Format[ApiResponse] = Format(apiResponseReads, apiResponseWrites)
  implicit lazy val apiSearchResponseFormat: Format[ApiSearchResponse] = Format(apiSearchResponseReads, apiSearchResponseWrites)
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
  implicit lazy val apiSearchResponseReads: Reads[ApiSearchResponse] = Json.reads[ApiSearchResponse]
}

object ApiWrites {
  implicit lazy val apiReferenceWrites: Writes[ApiReference] = Json.writes[ApiReference]
  implicit lazy val apiOnwardWrites: Writes[ApiOnward] = Json.writes[ApiOnward]
  implicit lazy val apiMetadataWrites: Writes[ApiMetadata] = Json.writes[ApiMetadata]
  implicit lazy val apiAssetWrites: Writes[ApiAsset] = Json.writes[ApiAsset]
  implicit lazy val apiSectionDataWrites: Writes[ApiSectionData] = Json.writes[ApiSectionData]
  implicit lazy val apiElementWrites: Writes[ApiElement] = Json.writes[ApiElement]
  implicit lazy val apiAuthorWrites: Writes[ApiAuthor] = Json.writes[ApiAuthor]
  implicit lazy val apiTagWrites: Writes[ApiTag] = Json.writes[ApiTag]
  implicit lazy val apiContentWrites: Writes[ApiContent] = Json.writes[ApiContent]
  implicit lazy val apiResponseWrites: Writes[ApiResponse] = Json.writes[ApiResponse]
  implicit lazy val apiSearchResponseWrites: Writes[ApiSearchResponse] = Json.writes[ApiSearchResponse]
}


