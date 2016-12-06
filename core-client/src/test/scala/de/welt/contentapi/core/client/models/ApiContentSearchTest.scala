package de.welt.contentapi.core.client.models

import org.scalatestplus.play.PlaySpec

class ApiContentSearchTest extends PlaySpec {

  val sectionPath: String = "/dickButt/"
  val homeSectionPath: String = "/dickButt/"
  val excludes: String = "-derpSection,-derpinaSection"
  val maxResultSize: Int = 10
  val page: Int = 1
  val contentType: String = "live"
  val subType: String = "ticker"
  val flags: String = "highlight"

  "ApiContentSearch" should {

    "use all declared fields for creating the query paremeters" in {
      val query: ApiContentSearch = ApiContentSearch(`type`= Some(MainTypeParam(contentType)))
      val expectedListOfParams: Seq[(String, String)] = List(("type", "live"))

      query.allParams.size mustBe query.getClass.getDeclaredFields.length
    }

    "create a list of key value strings from all passed parameters which can be passed into the model" in {
      val query: ApiContentSearch = ApiContentSearch(
        `type`= Some(MainTypeParam(contentType)),
        subType = Some(SubTypeParam(subType)),
        section = Some(SectionParam(sectionPath)),
        homeSection = Some(HomeSectionParam(homeSectionPath)),
        sectionExcludes = Some(SectionExcludes(excludes)),
        flags = Some(FlagParam(flags)),
        limit = Some(LimitParam(maxResultSize.toString)),
        page = Some(PageParam(page.toString))
      )

      val expectedListOfParams: Seq[(String, String)] = List(("type", "live"),
        ("subType", "ticker"),
        ("sectionPath", "/dickButt/"),
        ("sectionHome", "/dickButt/"),
        ("excludeSections", "-derpSection,-derpinaSection"),
        ("flag", "highlight"),
        ("pageSize", "10"),
        ("page", "1"))

      query.getAllParamsUnwrapped mustBe expectedListOfParams
    }

    "create a list of key value strings only from defined parameters" in {
      val query: ApiContentSearch = ApiContentSearch(
        `type`= Some(MainTypeParam(contentType))
      )
      val expectedListOfParams: Seq[(String, String)] = List(("type", "live"))

      query.getAllParamsUnwrapped mustBe expectedListOfParams
    }

  }
}
