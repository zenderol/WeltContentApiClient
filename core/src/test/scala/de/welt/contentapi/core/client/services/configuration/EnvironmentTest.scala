package de.welt.contentapi.core.client.services.configuration

import com.typesafe.config.ConfigFactory
import org.scalatestplus.play.PlaySpec

import scala.collection.mutable
import scala.util.Success

class EnvironmentTest extends PlaySpec {

  "Environment" must {

    "parse a valid configuration file" in {
      val parsed = Environment.parseVersionConfFile(ConfigFactory.parseString(
        """
          |build_info {
          |   module: "foo"
          |   dependencies: ["bar", "baz"]
          |}
        """.stripMargin))

      parsed mustBe Success("foo" → mutable.Buffer("bar", "baz"))
    }

    "find the root module in empty" in {
      Environment.findCurrentModule(Map()) mustBe None
    }

    /**
      * article
      *   |
      * common
      */
    "find the root module in an simple setup" in {
      Environment.findCurrentModule(Map(
        "common" → mutable.Buffer.empty,
        "article" → mutable.Buffer("common")
      )) mustBe Some("article")
    }

    /**
      * author
      *    |
      * article
      *   |
      * common
      */
    "find the root module deeper hierarchies" in {
      Environment.findCurrentModule(Map(
        "common" → mutable.Buffer.empty,
        "article" → mutable.Buffer("common"),
        "author" → mutable.Buffer("article")
      )) mustBe Some("author")
    }

    /**
      *        preview
      *       |      |
      * article     section
      *      |      |
      *      common
      */
    "find the root module broader hierarchies" in {
      Environment.findCurrentModule(Map(
        "common" → mutable.Buffer.empty,
        "article" → mutable.Buffer("common"),
        "section" → mutable.Buffer("common"),
        "preview" → mutable.Buffer("article", "section")
      )) mustBe Some("preview")
    }

  }

}
