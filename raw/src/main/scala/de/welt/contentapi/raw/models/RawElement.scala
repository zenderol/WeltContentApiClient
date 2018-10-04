package de.welt.contentapi.raw.models


/**
  * Elements used for Channel Site Building such as custom header, logo, or sponsoring text.
  *
  * @param id        Constant id that is only used to be able to reuse the ApiElement model
  * @param `type`    Type of element like: header, logo, or sponsoring text ...
  * @param assets    All assets of the element: image asset with url, video asset with url, poster, width, height
  */
case class RawElement(id: String = RawChannelElement.IdDefault,
                      `type`: String = RawChannelElement.TypeUnknown,
                      assets: Option[List[RawAsset]] = None) {
  lazy val unwrappedAssets: List[RawAsset] = assets.getOrElse(Nil)
}

/**
  * @param `type`   Type of the asset like: image, video
  * @param fields   Generic 'data/content' based on the type of asset. E.g. source, width, height, text
  */
case class RawAsset(`type`: String = RawChannelAsset.TypeUnknown,
                    fields: Option[Map[String, String]] = None) {
  lazy val unwrappedFields: Map[String, String] = fields.getOrElse(Map.empty[String, String])
}