package de.welt.contentapi.core.client.models

import java.time.Instant

import org.scalatestplus.play.PlaySpec

class ApiContentSearchTest extends PlaySpec {

  val id = List("-157673104", "-157078453")
  val sectionPath = "/dickButt/"
  val homeSectionPath = "/dickButt/"
  val excludes = List("-derpSection", "-derpinaSection")
  val maxResultSize = 10
  val page = 1
  val contentType = "live"
  val subType = "ticker"
  val flags = "highlight"
  val tag1 = "person-574114"
  val tag2 = "thing-574111"

  "ApiContentSearch" should {

    "use all declared fields for creating the query parameters" in {
      val query: ApiContentSearch = ApiContentSearch(`type` = MainTypeParam(List(contentType)))
      query.allParams.size mustBe query.getClass.getDeclaredFields.length
    }

    "create a list of key value strings from all passed parameters which can be passed into the model" in {
      val query: ApiContentSearch = ApiContentSearch(
        `type` = MainTypeParam(List(contentType)),
        subType = SubTypeParam(List(subType)),
        section = SectionParam(List(sectionPath)),
        homeSection = HomeSectionParam(List(homeSectionPath)),
        sectionExcludes = SectionExcludes(excludes),
        flag = FlagParam(List(flags)),
        tag = TagParam(List(tag1, tag2)),
        pageSize = PageSizeParam(maxResultSize),
        page = PageParam(page),
        id = IdParam(id)
      )

      val expectedListOfParams: Seq[(String, String)] = List(
        ("type", "live"),
        ("subType", "ticker"),
        ("sectionPath", "/dickButt/"),
        ("sectionHome", "/dickButt/"),
        ("excludeSections", "-derpSection,-derpinaSection"),
        ("flag", "highlight"),
        ("tag", "person-574114|thing-574111"),
        ("pageSize", "10"),
        ("page", "1"),
        ("id", "-157673104,-157078453")
      )

      query.getAllParamsUnwrapped mustBe expectedListOfParams
    }

    "create a list of key value strings only from defined parameters" in {
      val query: ApiContentSearch = ApiContentSearch(
        `type` = MainTypeParam(List(contentType))
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

    "date types are handled correctly" in {
      val dateParam = ApiContentSearch(fromDate = FromDateParam(Instant.ofEpochMilli(0))).getAllParamsUnwrapped
      dateParam must contain("fromDate" → "1970-01-01T00:00:00Z")
    }

    "tags are OR'ed with the '|' symbol" in {
      val allParams = ApiContentSearch(tag = TagParam(List(tag1, tag2))).getAllParamsUnwrapped
      allParams must contain("tag" → s"$tag1|$tag2")
    }
  }
}
