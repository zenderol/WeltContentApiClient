package de.welt.contentapi.pressed.client.converter

import de.welt.contentapi.raw.models.{RawChannelConfiguration, RawChannelHeader}
import de.welt.testing.TestHelper.raw.channel.{emptyWithId, emptyWithIdAndChildren}
import org.scalatestplus.play.PlaySpec

class Raw2ApiHeaderTest extends PlaySpec {

  //noinspection ScalaStyle
  trait HeaderTreeScope {

    // @formatter:off
    /**
      * *H* = has header configured
      * *M* = configured as Master (automatically if 1st level or by setting it via CMCF)
      *
      *         ( 0[root] *H* )
      *         |             |
      *      (10 *H* *M*)   (20 *M*)
      *         |             |
      *      (100 *H*)       (200)
      *        |              |
      *      (1000)       (2000 *H*)
      */
    // @formatter:on
    /**
      *
      * 1000 -> root
      */

    val node1000 = emptyWithId(1000)

    val node100 = emptyWithIdAndChildren(100, Seq(node1000))
    node100.config = RawChannelConfiguration(header = Some(RawChannelHeader(slogan = Some("100"))))

    val node10 = emptyWithIdAndChildren(10, Seq(node100))
    node10.config = RawChannelConfiguration(header = Some(RawChannelHeader(slogan = Some("10"))), master = true)

    /**
      *
      * 2000 -> root
      */

    val node2000 = emptyWithId(1100)
    node2000.config = RawChannelConfiguration(header = Some(RawChannelHeader(slogan = Some("2000"))))

    val node200 = emptyWithIdAndChildren(110, Seq(node2000))

    val node20 = emptyWithIdAndChildren(11, Seq(node200))
    node20.config = RawChannelConfiguration(master = true)

    val root = emptyWithIdAndChildren(0, Seq(node10, node20))
    root.config = RawChannelConfiguration(header = Some(RawChannelHeader(slogan = Some("root"))))

    root.updateParentRelations()

    import de.welt.contentapi.core.models.testImplicits.pathUpdater

    root.updatePaths()

    val converter: RawToApiConverter = new RawToApiConverter(new InheritanceCalculator())
  }

  "Header" must {

    "be used directly from channel if one is configured in cmcf" in new HeaderTreeScope {
      converter.calculateHeader(node2000).flatMap(_.slogan) mustBe node2000.config.header.flatMap(_.slogan)
    }

    "not inherit the header from a parent that is not master" in new HeaderTreeScope {
      converter.calculateHeader(node1000).flatMap(_.slogan) must not be node100.config.header.flatMap(_.slogan)
    }

    "be used from next master if channel has none" in new HeaderTreeScope {
      converter.calculateHeader(node1000) mustBe converter.calculateHeader(node10)
    }

    "inherit the EMPTY header from the next master" in new HeaderTreeScope {
      converter.calculateHeader(node200) mustBe None
    }

    "calculate the header for root only from its own config" in new HeaderTreeScope {
      converter.calculateHeader(root).flatMap(_.slogan) mustBe root.config.header.flatMap(_.slogan)
    }

    "not use a header for root if none is configured" in new HeaderTreeScope {
      root.config = RawChannelConfiguration()
      converter.calculateHeader(root) mustBe None
    }

    "use the valid config from a master channel" in new HeaderTreeScope {
      converter.calculateHeader(node10) mustBe defined
    }

    "use the empty config from a master channel" in new HeaderTreeScope {
      converter.calculateHeader(node20) mustBe None
    }
  }

}
