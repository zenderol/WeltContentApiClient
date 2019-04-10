package de.welt.contentapi.pressed.client.converter

import de.welt.testing.TestHelper.raw.channel._
import de.welt.testing.TestHelper.raw.configuration._
import org.scalatest.{FlatSpec, Matchers}


class Raw2ApiAdTagTests extends FlatSpec with Matchers {

  trait TestScopeAds {
    /**
      * {{{
      *      (      0[root]     )
      *       |               |
      *      (10) true    (11) false
      *       |               |
      *      (100) false   (111) true
      * }}}
      */
    val node100 = emptyWithIdAndConfig(100, withAds(false))
    val node10 = emptyWithIdAndChildrenAndConfig(10, Seq(node100), withAds(true))

    val node111 = emptyWithIdAndConfig(111, withAds(true))
    val node11 = emptyWithIdAndChildrenAndConfig(11, Seq(node111), withAds(false))

    val root = emptyWithIdAndChildrenAndConfig(0, Seq(node10, node11), withAds(true))

    root.updateParentRelations()
    import de.welt.contentapi.core.models.testImplicits.pathUpdater
    root.updatePaths()
  }

  val converter: RawToApiConverter = new RawToApiConverter(new InheritanceCalculator())

  "AdTag Calculator" must "calculate 'home' for root section (frontpage with path=='/' )" in new TestScopeAds {
    converter.calculatePathForVideoAdTag(root) shouldBe "home"
  }
  it must "calculate '10' as AdTag as node10 is the first in line with 'definesAdTag == true'" in new TestScopeAds {
    converter.calculatePathForAdTag(node100) shouldBe "10"
  }
  it must "calculate '10' as VideoAdTag as node10 is the first in line with 'definesVideoAdTag == true'" in new TestScopeAds {
    converter.calculatePathForVideoAdTag(node100) shouldBe "10"
  }
  it must "calculate 'sonstiges' AdTag because node11 has 'definesAdTag == false'" in new TestScopeAds {
    converter.calculatePathForAdTag(node11) shouldBe "sonstiges"
  }
  it must "calculate 'sonstiges' as VideoAdTag because node11 has 'definesVideoAdTag == false'" in new TestScopeAds {
    converter.calculatePathForVideoAdTag(node11) shouldBe "sonstiges"
  }

  it must "calculate '11/111' AdTag it has 'definesAdTag == true'" in new TestScopeAds {
    converter.calculatePathForAdTag(node111) shouldBe "11/111"
  }
  it must "calculate '11/111' as VideoAdTag because it has 'definesVideoAdTag == true'" in new TestScopeAds {
    converter.calculatePathForVideoAdTag(node111) shouldBe "11/111"
  }
}



