package de.welt.contentapi.pressed.client.converter

import de.welt.contentapi.raw.models.RawChannel
import de.welt.testing.TestHelper.raw.channel.{emptyWithIdAndChildrenAndMetadata, emptyWithIdAndMetadata}
import de.welt.testing.TestHelper.raw.metadata.withChangedBy
import org.scalatestplus.play.PlaySpec


class InheritanceCalculatorTest extends PlaySpec {

  "Inheritance for a single value" must {

    val EXPECTED_ROOT = "unique-root"
    val EXPECTED_FALLBACK = "fallback"
    val EXPECTED_MATCHING = "active-new"

    /**
      * {{{
      *      (              0[root]          )
      *       |               |             |
      *      (10) true       (11) false    (22) true
      *       |               |             |
      *      (100) false     (111) true    (222) true
      *       |               |             |
      *      (1000) false    (1111) false  (2222) false
      * }}}
      */
    //noinspection ScalaStyle
    sealed trait SingleValueInheritanceScope {
      val node1000: RawChannel = emptyWithIdAndMetadata(1000, withChangedBy(""))
      val node100: RawChannel = emptyWithIdAndChildrenAndMetadata(100, Seq(node1000), withChangedBy(""))
      val node10: RawChannel = emptyWithIdAndChildrenAndMetadata(10, Seq(node100), withChangedBy("active"))

      val node1111: RawChannel = emptyWithIdAndMetadata(1111, withChangedBy(""))
      val node111: RawChannel = emptyWithIdAndChildrenAndMetadata(111, Seq(node1111), withChangedBy("active"))
      val node11: RawChannel = emptyWithIdAndChildrenAndMetadata(11, Seq(node111), withChangedBy(""))

      val node2222: RawChannel = emptyWithIdAndMetadata(2222, withChangedBy(""))
      val node222: RawChannel = emptyWithIdAndChildrenAndMetadata(222, Seq(node2222), withChangedBy("active"))
      val node22: RawChannel = emptyWithIdAndChildrenAndMetadata(22, Seq(node222), withChangedBy("active--dick-butt"))

      val root: RawChannel = emptyWithIdAndChildrenAndMetadata(0, Seq(node10, node11, node22), withChangedBy("active"))

      root.updateParentRelations()

      import de.welt.contentapi.core.models.testImplicits.pathUpdater

      root.updatePaths()

      val inheritanceAction: InheritanceAction[String] = InheritanceAction[String](
        forRoot = c ⇒ EXPECTED_ROOT,
        forFallback = c ⇒ EXPECTED_FALLBACK,
        forMatching = c ⇒ c.metadata.changedBy + "-new"
      )
      val calculator: InheritanceCalculator = new InheritanceCalculator()

      def inherit(channel: RawChannel): String = calculator.forChannel[String](channel, inheritanceAction, c ⇒ c.metadata.changedBy.contains("active"))
    }

    "return unique value for the root-channel. Use-Case: unique value for root (Frontpage)." in new SingleValueInheritanceScope {
      inherit(root) mustBe EXPECTED_ROOT
    }

    "return fallback value for the last channel before root with no inheritance. Use-Case: protecting First-Level-Sections." in
      new SingleValueInheritanceScope {
        inherit(node11) mustBe EXPECTED_FALLBACK
      }

    "return value of predicate-matching ancestor" in new SingleValueInheritanceScope {
      inherit(node1000) mustBe EXPECTED_MATCHING
      inherit(node100) mustBe EXPECTED_MATCHING
      inherit(node1111) mustBe EXPECTED_MATCHING
    }

    "return a value without inheritance if the channel matches the predicate itself" in new SingleValueInheritanceScope {
      inherit(node111) mustBe EXPECTED_MATCHING
      inherit(node10) mustBe EXPECTED_MATCHING
    }

    "inherit from next parent instead of going up in tree after a matching channel" in new SingleValueInheritanceScope {
      inherit(node2222) mustBe EXPECTED_MATCHING
    }

  }

}
