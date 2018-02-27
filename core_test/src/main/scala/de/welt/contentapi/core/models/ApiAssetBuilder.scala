package de.welt.contentapi.core.models

class ApiAssetBuilder {

  //noinspection ScalaStyle
  private var a = ApiAsset(null)

  def withType(v: String): ApiAssetBuilder = {
    a = a.copy(`type` = v)
    this
  }

  def withFields(v: Map[String, String]): ApiAssetBuilder = {
    a = a.copy(fields = Some(v))
    this
  }

  //noinspection ScalaStyle
  def build: ApiAsset = {
    assert(null != a.`type`, "You must specify the mandatory field `type`")
    a
  }

}

object ApiAssetBuilder {

  def apply(): ApiAssetBuilder = new ApiAssetBuilder()

}