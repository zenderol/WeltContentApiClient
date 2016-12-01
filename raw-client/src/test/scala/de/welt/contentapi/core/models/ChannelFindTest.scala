package de.welt.contentapi.core.models

import de.welt.contentapi.raw.models.RawChannel
import de.welt.testing.testHelper
import org.scalatestplus.play.PlaySpec

class ChannelFindTest extends PlaySpec {

  import testImplicits.pathUpdater

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
    root.updatePaths()
  }

  "RawChannel find" must {
    "find node2 by path" in new Fixture {
      root.findByPath("/1/2/") must be(Some(node2))
    }

    "find root by path" in new Fixture {
      root.findByPath("/") must be(Some(root))
    }

    "non-existing path returns None" in new Fixture {
      root.findByPath("some-bullshit") must be(None)
    }

    "empty path returns root" in new Fixture {
      root.findByPath("") must be(Some(root))
    }

    "find node2 by escenicId" in new Fixture {
      root.findByEscenicId(2) must be(Some(node2))
    }

    "non-existing escenicId returns None" in new Fixture {
      root.findByEscenicId(-1) must be(None)
    }

    "can navigate from node 2 to the root node" in new Fixture {
      node2.root must be(root)
    }

    "root node finds itself as root node" in new Fixture {
      root.root must be(root)
    }
  }
}

object testImplicits {

  /**
    * calculate the complete path based on the channel's id
    *
    * @param current the [[RawChannel]] to be updated
    */
  implicit class pathUpdater(current: RawChannel) {
    def updatePaths(maybeParentPath: Option[String] = None): Unit = {

      current.id.path = maybeParentPath match {
        case None ⇒ "/"
        case Some(parentPath) ⇒ parentPath + current.id.escenicId + "/"
      }

      current.children.foreach { child ⇒
        child.updatePaths(Some(current.id.path))
      }

    }
  }

}
