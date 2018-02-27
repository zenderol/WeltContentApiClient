package de.welt.contentapi.core.models

class ApiElementBuilder {

  //noinspection ScalaStyle
  private var a = ApiElement(null, null, None, None)

  def withId(v: AnyVal): ApiElementBuilder = {
    a = a.copy(id = v.toString)
    this
  }

  def withType(v: String): ApiElementBuilder = {
    a = a.copy(`type` = v)
    this
  }

  def withAssets(v: List[ApiAsset]): ApiElementBuilder = {
    a = a.copy(assets = Some(v))
    this
  }

  def withRelations(v: List[String]): ApiElementBuilder = {
    a = a.copy(relations = Some(v))
    this
  }

  //noinspection ScalaStyle
  def build: ApiElement = {
    assert(null != a.id, "You must specify the mandatory field `id`")
    assert(null != a.`type`, "You must specify the mandatory field `type`")
    a
  }

}

object ApiElementBuilder {

  def apply(): ApiElementBuilder = new ApiElementBuilder()

}
