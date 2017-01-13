package de.welt.contentapi.core.client.models

import org.scalatestplus.play.PlaySpec

class ApiContentSearchTest extends PlaySpec {

  val sectionPath = "/dickButt/"
  val homeSectionPath = "/dickButt/"
  val excludes = List("-derpSection", "-derpinaSection")
  val maxResultSize = 10
  val page = 1
  val contentType = "live"
  val subType = "ticker"
  val flags = "highlight"

  "ApiContentSearch" should {

    "use all declared fields for creating the query parameters" in {
      val query: ApiContentSearch = ApiContentSearch(`type` = new MainTypeParam(contentType))
      query.allParams.size mustBe query.getClass.getDeclaredFields.length
    }

    "create a list of key value strings from all passed parameters which can be passed into the model" in {
      val query: ApiContentSearch = ApiContentSearch(
        `type` = new MainTypeParam(contentType),
        subType = new SubTypeParam(subType),
        section = new SectionParam(sectionPath),
        homeSection = new HomeSectionParam(homeSectionPath),
        sectionExcludes = SectionExcludes(excludes),
        flags = new FlagParam(flags),
        limit = LimitParam(maxResultSize),
        page = PageParam(page)
      )

      val expectedListOfParams: Seq[(String, String)] = List(
        ("type", "live"),
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
        `type` = new MainTypeParam(contentType)
      )
      val expectedListOfParams: Seq[(String, String)] = List(("type", "live"))

      query.getAllParamsUnwrapped mustBe expectedListOfParams
    }

    "main type is ','ed" in {
      val mainTypeParam = ApiContentSearch(MainTypeParam(List("main1", "main2"))).getAllParamsUnwrapped
      mainTypeParam must contain("type" → "main1,main2")
    }

    "home section is '|'ed" in {
      val homeParam = ApiContentSearch(homeSection = HomeSectionParam(List("home1", "home2"))).getAllParamsUnwrapped
      homeParam must contain("sectionHome" → "home1|home2")
    }
  }
}
