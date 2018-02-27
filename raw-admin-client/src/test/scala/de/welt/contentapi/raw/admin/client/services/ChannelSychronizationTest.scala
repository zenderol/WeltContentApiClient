package de.welt.contentapi.raw.admin.client.services

import de.welt.contentapi.raw.models.{ChannelUpdate, RawChannel, RawChannelCommercial}
import de.welt.testing.TestHelper
import org.scalatestplus.play.PlaySpec

//noinspection TypeAnnotation
class ChannelSyncTest extends PlaySpec {

  import implicitConversions._

  trait Fixture {

    /** CHILD 1 */
    val child1Config = TestHelper.raw.configuration.withAds(adsEnabled = true)
    val child1 = TestHelper.raw.channel.emptyWithIdAndConfig(1, child1Config)

    /** CHILD 2 */
    val child2Config = TestHelper.raw.configuration.withAds(adsEnabled = true)
    val child2 = TestHelper.raw.channel.emptyWithIdAndConfig(2, child2Config)

    /** CHILD 2 */
    val child3Config = TestHelper.raw.configuration.withAds(adsEnabled = true)
    val child3 = TestHelper.raw.channel.emptyWithIdAndConfig(3, child3Config)

    object twoChildren {

      /**
        * (0)
        * /  \
        * (1) (2)
        *
        */
      val root = TestHelper.raw.channel.emptyWithIdAndChildren(0, children = Seq(child1, child2))
      root.updateParentRelations()

      // data node for root
      val rootData = TestHelper.raw.configuration.withAds(adsEnabled = true)
      root.config = rootData
    }

    object twoChildrenMasterDataDiffers {
      /**
        * (0)
        * /  \
        * (1) (2)
        *
        */
      /** CHILD 1 */
      val child1Config = TestHelper.raw.configuration.withAds(adsEnabled = true)
      val modifiedChild1 = TestHelper.raw.channel.emptyWithIdAndConfig(1, child1Config)

      /** CHILD 2 */
      val child2Config = TestHelper.raw.configuration.withAds(adsEnabled = false)
      val modifiedChild2 = TestHelper.raw.channel.emptyWithIdAndConfig(2, child2Config)

      val root = TestHelper.raw.channel.emptyWithIdAndChildren(0, children = Seq(modifiedChild1, modifiedChild2))
      root.updateParentRelations()

      // data node for root
      val rootConfig = TestHelper.raw.configuration.withAds(adsEnabled = false)
      root.config = rootConfig
    }

    object threeChildren {

      /**
        * .     (0)
        * .  /   |   \
        * (1)   (2)   (3)*
        *
        */
      val root = TestHelper.raw.channel.emptyWithIdAndChildren(0, children = Seq(child1, child2, child3))
      root.updateParentRelations()

      // data node for root
      val rootData = TestHelper.raw.configuration.withAds(adsEnabled = true)
      root.config = rootData
    }

    object movedChild {

      val copyOf3 = child3.copyWithLabelAndPath("copyOf3-label", "/the-new-path")
      val copyOf2 = child2.copy(children = Seq(copyOf3))

      /**
        * .     (0)
        * .    /  \
        * .  (1)  (2)
        * .   |
        * .  (3)*
        */
      val root = TestHelper.raw.channel.emptyWithIdAndChildren(0, children = Seq(child1, copyOf2))
      root.updateParentRelations()

      // data node for root
      val rootConfig = TestHelper.raw.configuration.withAds(adsEnabled = true)
      root.config = rootConfig
    }

  }

  "ChannelTools" must {

    "support additions" must {

      "detect addition of new channels" in new Fixture {
        val update = ChannelTools.diff(twoChildren.root, threeChildren.root)
        update must be(ChannelUpdate(
          added = Seq(child3),
          deleted = Seq.empty,
          moved = Seq.empty
        ))
      }

      "apply additions to channel tree" in new Fixture {
        val root = twoChildren.root
        ChannelTools.merge(root, threeChildren.root)

        root.children must have size 3
      }

      "maintain the data for all the nodes" in new Fixture {

        val root = twoChildren.root
        ChannelTools.merge(root, threeChildren.root)

        root.config must be(twoChildren.rootData)
        root.findByEscenicId(1).map(_.config) must ===(Some(child1Config))
        root.findByEscenicId(2).map(_.config) must ===(Some(child2Config))
        root.findByEscenicId(3).map(_.config) must ===(Some(child3Config))
      }
    }

    "support deletions" should {
      "detect deletion of channels" in new Fixture {
        private val update = ChannelTools.diff(threeChildren.root, twoChildren.root)

        update must be(ChannelUpdate(
          added = Seq.empty,
          deleted = Seq(child3),
          moved = Seq.empty
        ))
      }

      "apply deletions to the tree" in new Fixture {
        val root = threeChildren.root
        ChannelTools.merge(root, twoChildren.root)

        root.children must have size 2
        root.findByEscenicId(3) must ===(None)
      }

      "maintain the data for all the nodes" in new Fixture {
        val root = threeChildren.root
        ChannelTools.merge(root, twoChildren.root)

        root.config must be(threeChildren.rootData)
        root.findByEscenicId(1).map(_.config) must ===(Some(child1Config))
        root.findByEscenicId(2).map(_.config) must ===(Some(child2Config))
      }
    }
    "support moving of channels" should {
      "detect moved channels" in new Fixture {
        private val update = ChannelTools.diff(threeChildren.root, movedChild.root)

        update must be(ChannelUpdate(
          added = Seq.empty,
          deleted = Seq.empty,
          moved = Seq(movedChild.copyOf3)
        ))
      }

      "apply moved items to the tree" in new Fixture {
        val root = threeChildren.root
        ChannelTools.merge(root, movedChild.root)

        root.children must have size 2
        root.children must not contain child3
        root.findByEscenicId(2).map(_.children).getOrElse(Nil) must contain(movedChild.copyOf3)
        root.findByEscenicId(3) must ===(Some(child3))
        val Some(x) = root.findByEscenicId(3)
        x.eq(child3) must ===(true)
      }

      "inverse apply moved items to the tree" in new Fixture {
        val root = movedChild.root
        ChannelTools.merge(root, threeChildren.root)

        root.children must have size 3
        root.findByEscenicId(3) must ===(Some(child3))
        val Some(x) = root.findByEscenicId(3)
        x.eq(movedChild.copyOf3) must ===(true)
      }

      "maintain the data for all the nodes" in new Fixture {

        val root = threeChildren.root
        ChannelTools.merge(root, movedChild.root)

        root.config must be(threeChildren.rootData)
        root.findByEscenicId(1).map(_.config) must ===(Some(child1Config))
        root.findByEscenicId(2).map(_.config) must ===(Some(child2Config))
        // the master data [path, label] from the other tree (movedChild.root) must be copied!
        root.findByEscenicId(3).map(_.id.label) must ===(Some(movedChild.copyOf3.id.label))
        root.findByEscenicId(3).map(_.id.path) must ===(Some(movedChild.copyOf3.id.path))
      }
    }

    "produce no changes" must {
      "for twoChildren example" in new Fixture {

        private val root = twoChildren.root
        ChannelTools.merge(root, root)
        root must be(new Fixture {}.twoChildren.root)
      }
      "for threeChildren example" in new Fixture {

        private val root = threeChildren.root
        ChannelTools.merge(root, root)
        root must be(new Fixture {}.threeChildren.root)
      }
      "for movedChild example" in new Fixture {

        private val root = movedChild.root
        ChannelTools.merge(root, root)
        root must be(new Fixture {}.movedChild.root)
      }
    }

    "support updates within the channels itself" must {
      "update the label" in new Fixture {

        private val root = twoChildren.root
        private val other = twoChildrenMasterDataDiffers.root
        ChannelTools.merge(root, other)

        root.id must be(other.id)
        root.id.label must be(other.id.label)

        root.findByEscenicId(1).map(_.id.label) must be(Some(twoChildrenMasterDataDiffers.modifiedChild1.id.label))
        root.findByEscenicId(2).map(_.id.label) must be(Some(twoChildrenMasterDataDiffers.modifiedChild2.id.label))
      }

      "update the path" in new Fixture {

        private val root = twoChildren.root
        private val other: RawChannel = twoChildrenMasterDataDiffers.root
        ChannelTools.merge(root, other)

        root.id must be(other.id)
        root.id.path must be(other.id.path)

        root.findByEscenicId(1).map(_.id.path) must be(Some(twoChildrenMasterDataDiffers.modifiedChild1.id.path))
        root.findByEscenicId(2).map(_.id.path) must be(Some(twoChildrenMasterDataDiffers.modifiedChild2.id.path))
      }

      "not update the ad data" in new Fixture {

        private val root = twoChildren.root
        private val other = twoChildrenMasterDataDiffers.root
        ChannelTools.merge(root, other)

        root.id must be(other.id)
        root.config.commercial must be(RawChannelCommercial(definesAdTag = true, definesVideoAdTag = true))

        root.findByEscenicId(1).map(_.config.commercial) must ===(Some(RawChannelCommercial(definesAdTag = true, definesVideoAdTag = true)))
        root.findByEscenicId(2).map(_.config.commercial) must ===(Some(RawChannelCommercial(definesAdTag = true, definesVideoAdTag = true)))
      }
    }
  }
}

object implicitConversions {

  implicit class rawChannelUtils(r: RawChannel) {

    def copyWithLabelAndPath(newLabel: String, newPath: String): RawChannel = r.copy(id = r.id.copy(label = newLabel, path = newPath))
  }

}
