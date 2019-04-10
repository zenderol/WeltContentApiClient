package de.welt.contentapi.pressed.client.converter

import de.welt.testing.TestHelper.raw.channel.{emptyWithId, emptyWithIdAndChildren, emptyWithIdAndChildrenAndConfig}
import de.welt.testing.TestHelper.raw.configuration.withTheme
import org.scalatest.{FlatSpec, Matchers}

class Raw2ThemeTest extends FlatSpec with Matchers {

  val EXPECTED_THEME: String = "icon"

  //noinspection ScalaStyle
  trait TestScopeAds {
    /**
      * {{{
      *      (    0[root] yes  )
      *       |               |
      *      (10) yes      (11) nope
      *       |
      *      (100) nope
      *       |
      *      (1000) nope
      * }}}
      */
    val node1000 = emptyWithId(1000)
    val node100 = emptyWithIdAndChildren(100, Seq(node1000))
    val node10 = emptyWithIdAndChildrenAndConfig(10, Seq(node100), withTheme(EXPECTED_THEME))

    val node11 = emptyWithId(11)

    val root = emptyWithIdAndChildrenAndConfig(0, Seq(node10, node11), withTheme(EXPECTED_THEME))

    root.updateParentRelations()

    import de.welt.contentapi.core.models.testImplicits.pathUpdater

    root.updatePaths()
  }

  val converter: RawToApiConverter = new RawToApiConverter(new InheritanceCalculator())

  "Theme Calculator" must "calculate `None` for root without a theme. The Frontpage has never a theme." in new TestScopeAds {
    converter.calculateTheme(root) shouldBe None
  }
  it must "calculate `Some(theme)` when the current channel has a theme" in new TestScopeAds {
    converter.calculateTheme(node10).flatMap(_.name) shouldBe Some(EXPECTED_THEME)
  }
  it must "calculate `Some(theme)` when the direct parent (except root) has a theme" in new TestScopeAds {
    converter.calculateTheme(node100).flatMap(_.name) shouldBe Some(EXPECTED_THEME)
  }
  it must "calculate `Some(theme)` when one of ancestors (except root) has a theme" in new TestScopeAds {
    converter.calculateTheme(node1000).flatMap(_.name) shouldBe Some(EXPECTED_THEME)
  }
  it must "calculate `None` when the current channel has no theme and it is a First-Level-Section" in new TestScopeAds {
    converter.calculateTheme(node11) shouldBe None
  }

}
