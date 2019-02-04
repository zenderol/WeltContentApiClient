package de.welt.contentapi.raw.admin.client.services

import java.time.Instant

import de.welt.contentapi.TestExecutionContext
import de.welt.contentapi.core.client.services.s3.S3Client
import de.welt.contentapi.raw.admin.client.models.SdpSectionData
import de.welt.contentapi.raw.client.services.RawTreeServiceImpl
import de.welt.contentapi.utils.Env.{Live, Preview}
import org.mockito.Mockito.when
import org.mockito.{ArgumentMatchers, Mockito}
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.{Configuration, Environment}

class SectionServiceTest extends PlaySpec with MockitoSugar {

  trait Fixture {

    val childOfChild = SdpSectionData("/child/child/", "childOfChild", None, Seq.empty, 2)
    val childOfRoot = SdpSectionData("/child/", "child", None, Seq(childOfChild), 1)
    val root = SdpSectionData("/", "root", None, Seq(childOfRoot), 0)

    val bucket = "le-bucket"
    val folder = "le-folder"
    val config = Configuration(
      RawTreeServiceImpl.bucketConfigKey → bucket,
      RawTreeServiceImpl.folderConfigKey → folder
    )

    val s3 = mock[S3Client]
    when(s3.getLastModified(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())) thenReturn None
    when(s3.getWithLastModified(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())) thenReturn None

    val legacyServiceMock = mock[SdpSectionDataService]
    when(legacyServiceMock.getSectionData) thenReturn root

    val service = new AdminSectionServiceImpl(config, s3, Environment.simple(), legacyServiceMock, TestExecutionContext.executionContext)
  }

  "SectionConfigurationService with empty initial s3 response" must {

    "load initial data from legacy service" in new Fixture {

      val channel = service.root(Preview).get

      channel.id.path must be("/")
      channel.id.label must be("root")
    }

    "have ad tags defined for depth 0~1" in new Fixture {

      val channel = service.root(Preview).get

      channel.config.commercial.definesAdTag must be(true)
      channel.children.map(_.config.commercial.definesAdTag) must contain(true)
    }

    "have not ad-tags for depth 2" in new Fixture {

      val channel = service.root(Preview).get

      val secondChild = channel.children.flatMap(_.children)
      secondChild.map(_.config.commercial.definesAdTag) must contain(false)
    }

    "store the data for both live and preview" in new Fixture {

      service.root(Preview)

      //      Mockito.verify(emptyS3ResponseMock, times(2)).get(ArgumentMatchers.eq(bucket), ArgumentMatchers.anyString())
      Mockito.verify(s3).putPrivate(ArgumentMatchers.eq(bucket), ArgumentMatchers.contains(Live.toString), ArgumentMatchers.anyString(), ArgumentMatchers.eq("application/json"))
      Mockito.verify(s3).putPrivate(ArgumentMatchers.eq(bucket), ArgumentMatchers.contains(Preview.toString), ArgumentMatchers.anyString(), ArgumentMatchers.eq("application/json"))

    }
  }
}

