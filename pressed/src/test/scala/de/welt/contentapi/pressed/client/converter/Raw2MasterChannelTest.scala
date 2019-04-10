package de.welt.contentapi.pressed.client.converter

import de.welt.contentapi.core.models.ApiReference
import de.welt.testing.TestHelper.raw.channel.{emptyWithId, emptyWithIdAndChildren, emptyWithIdAndChildrenAndConfig}
import de.welt.testing.TestHelper.raw.configuration.withMaster
import org.scalatestplus.play.PlaySpec

class Raw2MasterChannelTest extends PlaySpec {

  "Master Channel Calculator: Automatic calculated" must {

    //noinspection ScalaStyle
    trait AutomaticFlaggedMasterScope {
      /**
        * Info:
        * -A- == automatically a master channel (first level)
        *
        * {{{
        *      (      0 -A- [root]     )
        *       |               |
        *      (10 -A-)        (11 -A-)
        *       |
        *      (100)
        *       |
        *      (1000)
        * }}}
        */
      val node1000 = emptyWithId(1000)
      val node100 = emptyWithIdAndChildren(100, Seq(node1000))
      val node10 = emptyWithIdAndChildren(10, Seq(node100))

      val node11 = emptyWithIdAndChildren(11, Nil)

      val root = emptyWithIdAndChildren(0, Seq(node10, node11))

      root.updateParentRelations()

      import de.welt.contentapi.core.models.testImplicits.pathUpdater

      root.updatePaths()
      val converter: RawToApiConverter = new RawToApiConverter(new InheritanceCalculator())
    }

    val expectedRootMaster: ApiReference = ApiReference(label = Some("0"), href = Some("/"))
    val expectedNode10Master: ApiReference = ApiReference(label = Some("10"), href = Some("/10/"))
    val expectedNode11Master: ApiReference = ApiReference(label = Some("11"), href = Some("/11/"))

    "calculate `root` as master for the root channel." in new AutomaticFlaggedMasterScope {
      converter.calculateMasterReference(root) mustBe Some(expectedRootMaster)
    }

    "calculate `node10` as master for itself" in new AutomaticFlaggedMasterScope {
      converter.calculateMasterReference(node10) mustBe Some(expectedNode10Master)
    }

    "calculate `node10` as master for every direct child of `node10`" in new AutomaticFlaggedMasterScope {
      converter.calculateMasterReference(node100) mustBe Some(expectedNode10Master)
    }

    "calculate `node10` as master for every child-child (yo dawg) of `node10`" in new AutomaticFlaggedMasterScope {
      converter.calculateMasterReference(node1000) mustBe Some(expectedNode10Master)
    }

    "calculate `node11` as master for itself" in new AutomaticFlaggedMasterScope {
      converter.calculateMasterReference(node11) mustBe Some(expectedNode11Master)
    }
  }

  "Master Channel Calculator: Manual flagged" must {

    //noinspection ScalaStyle
    trait ManualFlaggedMasterScope {
      /**
        * Info:
        * -A- == automatically a master channel (first level)
        * -M- == manual set as master channel (CMCF GUI)
        *
        * {{{
        *      (      0 -A- [root]     )
        *       |               |
        *      (10 -A-)        (11 -A-)
        *       |
        *      (100 -M-)
        *       |
        *      (1000)
        *       |
        *      (10000)
        * }}}
        */
      val node10000 = emptyWithId(10000)
      val node1000 = emptyWithIdAndChildren(1000, Seq(node10000))
      val node100 = emptyWithIdAndChildrenAndConfig(100, Seq(node1000), withMaster(true))
      val node10 = emptyWithIdAndChildren(10, Seq(node100))

      val node11 = emptyWithIdAndChildren(11, Nil)

      val root = emptyWithIdAndChildren(0, Seq(node10, node11))

      root.updateParentRelations()

      import de.welt.contentapi.core.models.testImplicits.pathUpdater

      root.updatePaths()
      val converter: RawToApiConverter = new RawToApiConverter(new InheritanceCalculator())
    }

    val expectedRootMaster: ApiReference = ApiReference(label = Some("0"), href = Some("/"))
    val expectedNode10Master: ApiReference = ApiReference(label = Some("10"), href = Some("/10/"))
    val expectedNode100Master: ApiReference = ApiReference(label = Some("100"), href = Some("/10/100/"))
    val expectedNode11Master: ApiReference = ApiReference(label = Some("11"), href = Some("/11/"))

    "calculate `root` as master for the root channel." in new ManualFlaggedMasterScope {
      converter.calculateMasterReference(root) mustBe Some(expectedRootMaster)
    }

    "calculate `node10` as master for itself (automatic flagged)" in new ManualFlaggedMasterScope {
      converter.calculateMasterReference(node10) mustBe Some(expectedNode10Master)
    }

    "calculate `node100` as master for itself (manually flagged)" in new ManualFlaggedMasterScope {
      converter.calculateMasterReference(node100) mustBe Some(expectedNode100Master)
    }

    "calculate `node100` as master for every direct child of `node100`" in new ManualFlaggedMasterScope {
      converter.calculateMasterReference(node1000) mustBe Some(expectedNode100Master)
    }

    "calculate `node100` as master for every child-child (yo dawg) of `node100`" in new ManualFlaggedMasterScope {
      converter.calculateMasterReference(node10000) mustBe Some(expectedNode100Master)
    }

    "calculate `node11` as master for itself" in new ManualFlaggedMasterScope {
      converter.calculateMasterReference(node11) mustBe Some(expectedNode11Master)
    }
  }


}
