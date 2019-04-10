package de.welt.contentapi.pressed.client.converter

import de.welt.contentapi.core.models.ApiReference
import de.welt.contentapi.pressed.models.ApiHeaderConfiguration
import de.welt.contentapi.raw.models.{RawChannelConfiguration, RawChannelHeader, RawSectionReference}
import de.welt.testing.TestHelper.raw.channel.{emptyWithId, emptyWithIdAndChildren}
import org.scalatestplus.play.PlaySpec

class Raw2ApiHeaderTest extends PlaySpec {

  val converter: RawToApiConverter = new RawToApiConverter(new InheritanceCalculator())

  //noinspection ScalaStyle
  trait HeaderTreeScope {

    // @formatter:off

    /**
      * *H* = nonEmpty header
      * *h* = header equals the default constructor
      * *M* = is explicit master
      * *m* = is implicit master, because 1st level child of root
      *
      *
      *         (    0[root]   *H*    )
      *         |            \        \
      *      (10 *HM*)    (20 *M*)   (30 *m*)
      *        |              \
      *      (100)         (200 *H*)
      *       |                \
      *     (1000 *h*)     (2000 *H*)
      */

    // @formatter:on

    /** 1XXX */
    val node1000 = emptyWithId(1000)
    node1000.config = RawChannelConfiguration(header = Some(RawChannelHeader()))

    val node100 = emptyWithIdAndChildren(100, Seq(node1000))

    val node10 = emptyWithIdAndChildren(10, Seq(node100))
    node10.config = RawChannelConfiguration(header = Some(RawChannelHeader(
      slogan = Some("slogan"),
      logo = Some("logo"),
      label = Some("label"),
      sectionReferences = Some(Seq(RawSectionReference(Some("foo"), Some("bar")))),
      headerReference = Some(RawSectionReference(label = Some("foo"))),
      adIndicator = true,
      hidden = true,
      sloganReference = Some(RawSectionReference(label = Some("foo")))
    )), master = true)

    /** 2XXX */
    val node2000 = emptyWithId(200)
    node2000.config = RawChannelConfiguration(header = None)

    val node200 = emptyWithIdAndChildren(200, Seq(node2000))
    node200.config = RawChannelConfiguration(header = Some(RawChannelHeader(slogan = Some("200"))))

    val node20 = emptyWithIdAndChildren(20, Seq(node200))
    node20.config = RawChannelConfiguration(master = true, header = None)

    /** 3X */
    val node30 = emptyWithId(30)

    /** root */
    val root = emptyWithIdAndChildren(0, Seq(node10, node20, node30))
    root.config = RawChannelConfiguration(header = Some(RawChannelHeader(slogan = Some("root_slogan"), logo = Some("root_logo"))))
    root.updateParentRelations()

    import de.welt.contentapi.core.models.testImplicits.pathUpdater

    root.updatePaths()
  }

  "Header Conversion WITHOUT master inheritance" must {

    "be chosen if channel defines a slogan" in {
      val channel = emptyWithId(1000)
      channel.config = RawChannelConfiguration(header = Some(RawChannelHeader(slogan = Some("slogan"))))
      converter.calculateHeader(channel) mustBe Some(ApiHeaderConfiguration(slogan = Some("slogan")))
    }

    "be chosen if channel defines a sloganReference" in {
      val channel = emptyWithId(1000)
      channel.config = RawChannelConfiguration(header = Some(RawChannelHeader(sloganReference = Some(RawSectionReference(path = Some("/path/"))))))
      converter.calculateHeader(channel) mustBe Some(ApiHeaderConfiguration(sloganReference = Some(ApiReference(href = Some("/path/")))))
    }

    "be chosen if channel defines a logo" in {
      val channel = emptyWithId(1000)
      channel.config = RawChannelConfiguration(header = Some(RawChannelHeader(logo = Some("logo"))))
      converter.calculateHeader(channel) mustBe Some(ApiHeaderConfiguration(logo = Some("logo")))
    }

    "be chosen if channel defines a label" in {
      val channel = emptyWithId(1000)
      channel.config = RawChannelConfiguration(header = Some(RawChannelHeader(label = Some("label"))))
      converter.calculateHeader(channel) mustBe Some(ApiHeaderConfiguration(label = Some("label")))
    }

    "be chosen if channel defines a label/logo reference" in {
      val channel = emptyWithId(1000)
      channel.config = RawChannelConfiguration(header = Some(RawChannelHeader(headerReference = Some(RawSectionReference(path = Some("/path/"))))))
      converter.calculateHeader(channel) mustBe Some(ApiHeaderConfiguration(headerReference = Some(ApiReference(href = Some("/path/")))))
    }

    "be chosen if channel defines a sub navigation (SectionReferences)" in {
      val channel = emptyWithId(1000)
      channel.config = RawChannelConfiguration(header = Some(RawChannelHeader(sectionReferences = Some(Seq(RawSectionReference(path = Some("/path/")))))))
      converter.calculateHeader(channel) mustBe Some(ApiHeaderConfiguration(sectionReferences = Some(Seq(ApiReference(href = Some("/path/"))))))
    }

    "be chosen if channel defines that the header should be hidden" in {
      val channel = emptyWithId(1000)
      channel.config = RawChannelConfiguration(header = Some(RawChannelHeader(hidden = true)))
      converter.calculateHeader(channel) mustBe Some(ApiHeaderConfiguration(hidden = Some(true)))
    }
  }

  "Header Conversion WITH master inheritance" must {

    "be chosen if values are equal to constructor defaults (`hidden` and `adIndicator` with value `false`)" in new HeaderTreeScope {
      converter.calculateHeader(node1000) mustBe converter.calculateHeader(node10)
    }

    "be chosen if channel ONLY defines the adIndicator as true because this field is misplaced" in new HeaderTreeScope {
      node1000.config = RawChannelConfiguration(header = Some(RawChannelHeader(adIndicator = true)))
      converter.calculateHeader(node1000) mustBe converter.calculateHeader(node10)
    }

    "be chosen if channel has no RawChannelHeader defined at all" in new HeaderTreeScope {
      node1000.config = RawChannelConfiguration(header = None)
      converter.calculateHeader(node1000) mustBe converter.calculateHeader(node10)
    }
  }

  "Header" must {

    "not be inherited from parent that has a header but is not master" in new HeaderTreeScope {
      converter.calculateHeader(node2000) must not be converter.calculateHeader(node200)
    }

    "be inherited from next master even if the master has an empty header" in new HeaderTreeScope {
      converter.calculateHeader(node2000) mustBe empty
    }

    "be empty for a first level section that has no header (no inheritance from root)" in new HeaderTreeScope {
      converter.calculateHeader(node30) mustBe empty
    }
  }

}
