package de.welt.contentapi.core.models

import java.time.Instant

import de.welt.contentapi.raw.models.RawChannelCommercial
import de.welt.contentapi.raw.client.models.SdpSectionData
import org.scalatestplus.play.PlaySpec

//noinspection TypeAnnotation
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
      channel.id.label must be("root")
    }

    "lastMod will be copied if present" in {
      val now = Instant.now
      val channel = SdpSectionData("/", "", Some(now.toEpochMilli.toString), Seq.empty, -1).toChannel

      channel.metadata.lastModifiedDate must be(now.toEpochMilli)
    }

    "lastMod will be created if missing" in {
      val now = Instant.now
      val channel = SdpSectionData("/", "", None, Seq.empty, -1).toChannel

      channel.metadata.lastModifiedDate must be >= now.toEpochMilli
    }

    "children must be present" in new Fixture {
      val channel = root.toChannel

      channel.hasChildren must be(true)

      val child = channel.children.head
      child.id.path must be("/child/")
    }

    "define ad-tags for depth 0 to 1" in new Fixture {
      val channel = root.toChannel

      channel.config.commercial must be(RawChannelCommercial(definesAdTag = true, definesVideoAdTag = true))
      channel.children.map(_.config.commercial) must contain(RawChannelCommercial(definesAdTag = true, definesVideoAdTag = true))
    }

    "have not ad-tags for depth 2" in new Fixture {
      val channel = root.toChannel

      val secondChild = channel.children.flatMap(_.children)
      secondChild.map(_.config.commercial) must contain(RawChannelCommercial(definesAdTag = false, definesVideoAdTag = false))
    }
  }

}
