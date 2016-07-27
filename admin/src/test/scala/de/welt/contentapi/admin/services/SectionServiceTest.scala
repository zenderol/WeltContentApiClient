package de.welt.contentapi.admin.services

import de.welt.contentapi.admin.models.SdpSectionData
import de.welt.contentapi.client.services.configuration.ContentClientConfigImpl
import de.welt.contentapi.client.services.contentapi.admin.LegacySectionService
import de.welt.contentapi.client.services.s3.S3
import de.welt.contentapi.core.models.{Live, Preview}
import de.welt.meta.DisabledCache
import org.mockito.Mockito._
import org.mockito.{Matchers, Mockito}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.{Configuration, Environment}

class SectionServiceTest extends PlaySpec with MockitoSugar {

  trait Fixture {

    val childOfChild = SdpSectionData("/child/child/", "childOfChild", None, Seq.empty, 2)
    val childOfRoot = SdpSectionData("/child/", "child", None, Seq(childOfChild), 1)
    val root = SdpSectionData("/", "root", None, Seq(childOfRoot), 0)

    val bucket = "le-bucket"
    val file = "le-file"
    val config = new ContentClientConfigImpl(Configuration(
      "funkotron.aws.s3.janus.bucket" → bucket,
      "funkotron.aws.s3.janus.file" → file
    ))

    val emptyS3ResponseMock = mock[S3]
    when(emptyS3ResponseMock.get(Matchers.eq(bucket), Matchers.anyString())) thenReturn None

    val legacyServiceMock = mock[LegacySectionService]
    when(legacyServiceMock.getSectionData) thenReturn root

    val service = new AdminSectionServiceImpl(config, emptyS3ResponseMock, Environment.simple(), legacyServiceMock, DisabledCache)
  }

  "SectionConfigurationService with empty initial s3 response" must {

    "load initial data from legacy service" in new Fixture {

      val channel = service.root(Preview).get

      channel.id.path must be("/")
      channel.data.label must be("root")
    }

    "have ad tags defined for depth 0~1" in new Fixture {

      val channel = service.root(Preview).get

      channel.data.adData.definesAdTag must be(true)
      channel.children.map(_.data.adData.definesAdTag) must contain(true)
    }

    "have not ad-tags for depth 2" in new Fixture {

      val channel = service.root(Preview).get

      val secondChild = channel.children.flatMap(_.children)
      secondChild.map(_.data.adData.definesAdTag) must contain(false)
    }

    "store the data for both live and preview" in new Fixture {

      service.root(Preview)

      Mockito.verify(emptyS3ResponseMock).get(Matchers.eq(bucket), Matchers.anyString())
      Mockito.verify(emptyS3ResponseMock).putPrivate(Matchers.eq(bucket), Matchers.contains(Live.toString), Matchers.anyString(), Matchers.eq("application/json"))
      Mockito.verify(emptyS3ResponseMock).putPrivate(Matchers.eq(bucket), Matchers.contains(Preview.toString), Matchers.anyString(), Matchers.eq("application/json"))

      Mockito.verifyNoMoreInteractions(emptyS3ResponseMock)
    }
  }
}

