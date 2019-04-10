package de.welt.contentapi.pressed.client.converter

import de.welt.contentapi.core.models.ApiReference
import de.welt.contentapi.pressed.models.ApiChannel
import de.welt.testing.TestHelper.raw.channel._
import org.scalatest.{FlatSpec, Matchers}

class Raw2ApiBreadcrumbTests extends FlatSpec with Matchers {

  trait TestScopeHierarchy {
    /**
      * {{{
      *       (    0[root]    )
      *       |               |
      *      (10)           (11)
      *       |               |
      *      (100)        (111)
      * }}}
      */
    val node100 = emptyWithId(100)
    val node10 = emptyWithIdAndChildren(10, Seq(node100))

    val node111 = emptyWithId(111)
    val node11 = emptyWithIdAndChildren(11, Seq(node111))

    val root = emptyWithIdAndChildren(0, Seq(node10, node11))

    root.updateParentRelations()
    import de.welt.contentapi.core.models.testImplicits.pathUpdater
    root.updatePaths()
  }

  trait TestScopeConfiguration

  val converter: RawToApiConverter = new RawToApiConverter(new InheritanceCalculator())
    "Breadcrumb" must "must be sorted from root to leafs with all 3 levels" in new TestScopeHierarchy {
      val result: ApiChannel = converter.apiChannelFromRawChannel(rawChannel = node111)
      val breadcrumb: Seq[ApiReference] = result.breadcrumb.getOrElse(Seq.empty)
      breadcrumb.map(_.href.getOrElse("")) shouldBe Seq("/", "/11/", "/11/111/")
    }

    "ApiSectionReference" must "have label and href from the RawChannel" in new TestScopeHierarchy {
      val apiReferenceFussball: ApiReference = converter.getApiSectionReferenceFromRawChannel(rawChannel = node111)
      apiReferenceFussball.href shouldBe Some("/11/111/")
      apiReferenceFussball.label shouldBe Some("111")
    }
}



