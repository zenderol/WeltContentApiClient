package de.welt.contentapi.core.models

import de.welt.testing.testHelper
import org.scalatestplus.play.PlaySpec

//noinspection TypeAnnotation
class ChannelBreadcrumbTest extends PlaySpec {

  trait Fixture {

    /**
      * {{{
      *      (0)
      *       |
      *      (1)
      *       |
      *      (2)
      * }}}
      */
    val node2 = testHelper.raw.channel.emptyWithId(2)
    val node1 = testHelper.raw.channel.emptyWithIdAndChildren(1, Seq(node2))
    val root = testHelper.raw.channel.emptyWithIdAndChildren(0, Seq(node1))

    root.updateParentRelations()
  }

  "RawChannel breadcrumb" must {

    "generate correct breadcrumb for depth 1" in new Fixture {

      val breadcrumb = root.getBreadcrumb

      breadcrumb mustBe Seq(root)
    }

    "generate correct breadcrumb for depth 2" in new Fixture {

      val breadcrumb = node1.getBreadcrumb

      breadcrumb mustBe Seq(root, node1)
    }

    "generate correct breadcrumb for depth 3" in new Fixture {

      val breadcrumb = node2.getBreadcrumb

      breadcrumb mustBe Seq(root, node1, node2)
    }

    "rewrites the label of the root to Home" in new Fixture {

      val breadcrumb = node2.getBreadcrumb

      breadcrumb.head.id.label must be("Home")
    }
  }
}
