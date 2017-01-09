package de.welt.contentapi.pressed.client.converter

import de.welt.testing.TestHelper.raw.channel.{emptyWithIdAndChildrenAndConfig, emptyWithIdAndConfig}
import de.welt.testing.TestHelper.raw.configuration.withBrand
import org.scalatest.{FlatSpec, Matchers}

class Raw2BrandTests extends FlatSpec with Matchers {

  //noinspection ScalaStyle
  trait TestScopeAds {
    /**
      * {{{
      *      (      0[root]     )
      *       |               |
      *      (10) true    (11) false
      *       |
      *      (100) false
      *       |
      *      (1000) false
      * }}}
      */
    val node1000 = emptyWithIdAndConfig(1000, withBrand(false))
    val node100 = emptyWithIdAndChildrenAndConfig(100, Seq(node1000), withBrand(false))
    val node10 = emptyWithIdAndChildrenAndConfig(10, Seq(node100), withBrand(true))

    val node11 = emptyWithIdAndChildrenAndConfig(11, Nil, withBrand(false))

    val root = emptyWithIdAndChildrenAndConfig(0, Seq(node10, node11), withBrand(true))

    root.updateParentRelations()

    import de.welt.contentapi.core.models.testImplicits.pathUpdater

    root.updatePaths()
  }

  val converter: RawToApiConverter = new RawToApiConverter(new InheritanceCalculator())

  "Brand Calculator" must "calculate `false` for the root channel. The Frontpage is never a Brand." in new TestScopeAds {
    converter.calculateBrand(root) shouldBe false
  }
  it must "calculate `true` when the current channel is flagged as a brand" in new TestScopeAds {
    converter.calculateBrand(node10) shouldBe true
  }
  it must "calculate `true` when the direct parent (except root) is flagged as a brand" in new TestScopeAds {
    converter.calculateBrand(node100) shouldBe true
  }
  it must "calculate `true` when one of ancestors (except root) is flagged as a brand" in new TestScopeAds {
    converter.calculateBrand(node1000) shouldBe true
  }
  it must "calculate 'false' when the current channel is not flagged as a brand and its a First-Level-Section" in new TestScopeAds {
    converter.calculateBrand(node11) shouldBe false
  }

}
