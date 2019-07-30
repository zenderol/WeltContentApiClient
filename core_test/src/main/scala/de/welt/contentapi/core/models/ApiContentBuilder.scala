package de.welt.contentapi.core.models

class ApiContentBuilder {

  //noinspection ScalaStyle
  private var apiContent = ApiContent(null, null)

  def withWebUrl(v: String): ApiContentBuilder = {
    apiContent = apiContent.copy(webUrl = v)
    this
  }

  def withId(v: AnyVal): ApiContentBuilder = {
    apiContent = apiContent.copy(id = Some(v.toString))
    this
  }

  def withType(v: String): ApiContentBuilder = {
    apiContent = apiContent.copy(`type` = v)
    this
  }

  def withElements(e: List[ApiElement]): ApiContentBuilder = {
    apiContent = apiContent.copy(elements = Some(e))
    this
  }

  def withElements(e: Option[List[ApiElement]]): ApiContentBuilder = {
    apiContent = apiContent.copy(elements = e)
    this
  }

  def withSectionData(e: ApiSectionData): ApiContentBuilder = {
    apiContent = apiContent.copy(sections = Some(e))
    this
  }

  def withFields(f: Map[String, String]): ApiContentBuilder = {
    apiContent = apiContent.copy(fields = Some(f))
    this
  }

  //noinspection ScalaStyle
  def build: ApiContent = {
    assert(null != apiContent.webUrl, "You must specify the mandatory field `webUrl`")
    assert(null != apiContent.`type`, "You must specify the mandatory field `type`")
    apiContent
  }
}

object ApiContentBuilder {

  def apply(): ApiContentBuilder = new ApiContentBuilder()

  def apply(id: AnyVal, webUrl: String): ApiContentBuilder = new ApiContentBuilder().withId(id).withWebUrl(webUrl)

}