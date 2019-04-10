package de.welt.contentapi.pressed.client.converter

import de.welt.contentapi.core.models.{ApiAsset, ApiElement, ApiReference}
import de.welt.contentapi.pressed.models.{ApiSiteBuildingConfiguration}
import de.welt.contentapi.raw.models
import de.welt.contentapi.raw.models._
import de.welt.testing.TestHelper.raw.channel.{emptyWithId, emptyWithIdAndChildren}
import org.scalatestplus.play.PlaySpec

class Raw2ApiSiteBuildingTest extends PlaySpec {

  val converter: RawToApiConverter = new RawToApiConverter(new InheritanceCalculator())

  //noinspection ScalaStyle
  trait SiteBuildingTreeScope {

    // @formatter:off

    /**
      * *S* = nonEmpty site building
      * *s* = site building equals the default constructor
      * *M* = is explicit master
      * *m* = is implicit master, because 1st level child of root
      *
      *
      *         (    0[root]   *S*    )
      *         |            \        \
      *      (10 *SM*)    (20 *M*)   (30 *m*)
      *        |              \
      *      (100)         (200 *S*)
      *       |                \
      *     (1000 *s*)     (2000 *S*)
      */

    // @formatter:on

    /** 1XXX */
    val node1000 = emptyWithId(1000)
    node1000.config = RawChannelConfiguration(siteBuilding = Some(RawChannelSiteBuilding()))

    val node100 = emptyWithIdAndChildren(100, Seq(node1000))

    val node10 = emptyWithIdAndChildren(10, Seq(node100))
    node10.config = RawChannelConfiguration(siteBuilding = Some(RawChannelSiteBuilding(
      fields = Some(Map("key1" -> "value2", "key2" -> "value2")),
      sub_navigation = Some(Seq(RawSectionReference(Some("Label"), Some("/Path/")))),
      elements = Some(Seq(
        RawElement(
          id = RawChannelElement.IdDefault,
          `type` = "mood",
          assets = Some(List(
            RawAsset(
              `type` = "image",
              fields = Some(Map("key1" -> "value2", "key2" -> "value2"))
            )
          ))
        )
      ))
    )), master = true)

    /** 2XXX */
    val node2000 = emptyWithId(200)
    node2000.config = RawChannelConfiguration(siteBuilding = None)

    val node200 = emptyWithIdAndChildren(200, Seq(node2000))
    node200.config = RawChannelConfiguration(siteBuilding = Some(RawChannelSiteBuilding(fields = Some(Map("header_label" -> "Label")))))

    val node20 = emptyWithIdAndChildren(20, Seq(node200))
    node20.config = RawChannelConfiguration(master = true, siteBuilding = None)

    /** 3X */
    val node30 = emptyWithId(30)

    /** root */
    val root = emptyWithIdAndChildren(0, Seq(node10, node20, node30))
    root.config = RawChannelConfiguration(siteBuilding = Some(RawChannelSiteBuilding(
      fields = Some(Map("header_slogan" -> "Slogan")),
      sub_navigation = Some(Seq(RawSectionReference(Some("Label"), Some("/Path/"))))
    )))
    root.updateParentRelations()

    import de.welt.contentapi.core.models.testImplicits.pathUpdater

    root.updatePaths()
  }

  "Sitebuilding Conversion WITHOUT master inheritance" must {

    "be chosen if channel defines a sitebuilding field" in {
      val channel = emptyWithId(1000)
      channel.config = RawChannelConfiguration(siteBuilding = Some(models.RawChannelSiteBuilding(fields = Some(Map("header_label" -> "Label")))))
      converter.calculateSiteBuilding(channel) mustBe Some(ApiSiteBuildingConfiguration(fields = Some(Map("header_label" -> "Label"))))
    }

    "be chosen if channel defines a sitebuilding element" in {

      val channel = emptyWithId(1000)
      channel.config = RawChannelConfiguration(siteBuilding = Some(models.RawChannelSiteBuilding(
        elements = Some(Seq(
          RawElement(
            id = RawChannelElement.IdDefault,
            `type` = "mood",
            assets = Some(List(
              RawAsset(
                `type` = "image",
                fields = Some(Map("key1" -> "value2", "key2" -> "value2"))
              )
            ))
          )
        )))
      ))
      converter.calculateSiteBuilding(channel) mustBe Some(ApiSiteBuildingConfiguration(
        elements = Some(Seq(
          ApiElement(
            id = RawChannelElement.IdDefault,
            `type` = "mood",
            assets = Some(List(
              ApiAsset(
                `type` = "image",
                fields = Some(Map("key1" -> "value2", "key2" -> "value2"))
              )
            ))
          )
        ))
      ))
    }

    "be chosen if channel defines a sub navigation (SectionReferences)" in {
      val channel = emptyWithId(1000)
      channel.config = RawChannelConfiguration(siteBuilding = Some(RawChannelSiteBuilding(sub_navigation = Some(Seq(RawSectionReference(path = Some("/path/")))))))
      converter.calculateSiteBuilding(channel) mustBe Some(ApiSiteBuildingConfiguration(sub_navigation = Some(Seq(ApiReference(href = Some("/path/"))))))
    }
  }

  "Site Building Conversion WITH master inheritance" must {

    "be chosen if values are equal to constructor defaults (empty site building configuration)" in new SiteBuildingTreeScope {
      converter.calculateSiteBuilding(node1000) mustBe converter.calculateSiteBuilding(node10)
    }
  }

  "Site Building" must {

    "not be inherited from parent that has a site building configuration but is not master" in new SiteBuildingTreeScope {
      converter.calculateSiteBuilding(node2000) must not be converter.calculateSiteBuilding(node200)
    }

    "be inherited from next master even if the master has an empty site building configuration" in new SiteBuildingTreeScope {
      converter.calculateSiteBuilding(node2000) mustBe empty
    }

    "be empty for a first level section that has no site building configuration (no inheritance from root)" in new SiteBuildingTreeScope {
      converter.calculateSiteBuilding(node30) mustBe empty
    }
  }
}
