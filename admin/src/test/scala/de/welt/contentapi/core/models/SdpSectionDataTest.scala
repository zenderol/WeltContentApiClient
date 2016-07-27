package de.welt.contentapi.core.models

import java.time.Instant

import de.welt.contentapi.admin.models.SdpSectionData
import org.scalatestplus.play.PlaySpec

class SdpSectionDataTest extends PlaySpec {

  trait Fixture {
    val childOfChild = SdpSectionData("/child/child/", "childOfChild", None, Seq.empty, 2)
    val childOfRoot = SdpSectionData("/child/", "child", None, Seq(childOfChild), 1)
    val root = SdpSectionData("/", "root", None, Seq(childOfRoot), 0)
  }

  "SdpSectionData" must {

    "transform the path and label" in new Fixture {
      val channel = root.toChannel

      channel.id.path must be("/")
      channel.data.label must be("root")
    }

    "set the type to non-virtual" in new Fixture {
      val channel = root.toChannel

      channel.id.isVirtual must be(false)
    }

    "lastMod will be copied if present" in {
      val now = Instant.now
      val channel = SdpSectionData("/", "", Some(now.toEpochMilli.toString), Seq.empty, -1).toChannel

      channel.lastModifiedDate must be(now.toEpochMilli)
    }

    "lastMod will be created if missing" in {
      val now = Instant.now
      val channel = SdpSectionData("/", "", None, Seq.empty, -1).toChannel

      channel.lastModifiedDate must be >= now.toEpochMilli
    }

    "children must be present" in new Fixture {
      val channel = root.toChannel

      channel.hasChildren must be(true)

      val child = channel.children.head
      child.id.path must be("/child/")
    }

    "define ad-tags for depth 0 to 1" in new Fixture {
      val channel = root.toChannel

      channel.data.adData.definesAdTag must be(true)
      channel.children.map(_.data.adData.definesAdTag) must contain(true)
    }

    "have not ad-tags for depth 2" in new Fixture {
      val channel = root.toChannel

      val secondChild = channel.children.flatMap(_.children)
      secondChild.map(_.data.adData.definesAdTag) must contain(false)
    }
  }

}
