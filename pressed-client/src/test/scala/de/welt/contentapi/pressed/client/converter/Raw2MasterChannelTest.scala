package de.welt.contentapi.pressed.client.converter

import de.welt.contentapi.core.models.ApiReference
import de.welt.testing.TestHelper.raw.channel.{emptyWithId, emptyWithIdAndChildren}
import org.scalatest.{FlatSpec, Matchers}

class Raw2MasterChannelTest extends FlatSpec with Matchers {

  //noinspection ScalaStyle
  trait TestScope {
    /**
      * {{{
      *      (      0[root]     )
      *       |               |
      *      (10)            (11)
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

  "Master Channel Calculator" must "calculate `root` as master for the root channel." in new TestScope {
    converter.calculateMaster(root) shouldBe Some(expectedRootMaster)
  }
  it must "calculate `node10` as master for itself" in new TestScope {
    converter.calculateMaster(node10) shouldBe Some(expectedNode10Master)
  }
  it must "calculate `node10` as master for every direct child of `node10`" in new TestScope {
    converter.calculateMaster(node100) shouldBe Some(expectedNode10Master)
  }
  it must "calculate `node10` as master for every child-child (yo dawg) of `node10`" in new TestScope {
    converter.calculateMaster(node1000) shouldBe Some(expectedNode10Master)
  }
  it must "calculate `node11` as master for itself" in new TestScope {
    converter.calculateMaster(node11) shouldBe Some(expectedNode11Master)
  }

}
