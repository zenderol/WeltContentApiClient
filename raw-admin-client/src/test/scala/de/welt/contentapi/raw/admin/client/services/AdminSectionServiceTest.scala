package de.welt.contentapi.raw.admin.client.services
import java.time.Instant

import de.welt.contentapi.TestExecutionContext
import de.welt.contentapi.core.client.services.s3.S3Client
import de.welt.contentapi.raw.client.services.RawTreeServiceImpl
import de.welt.contentapi.raw.models.FullRawChannelWrites.channelWrites
import de.welt.contentapi.raw.models.{RawChannelConfiguration, RawChannelHeader, RawChannelStageConfiguration, RawChannelStageCustomModule}
import de.welt.contentapi.utils.Env.Live
import de.welt.testing.TestHelper.raw.channel.emptyWithId
import org.mockito.{Matchers, Mockito}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.{Configuration, Environment}

class AdminSectionServiceTest extends PlaySpec with MockitoSugar {

  trait Fixture {
    val s3 = mock[S3Client]
    val configData = Map(
      RawTreeServiceImpl.bucketConfigKey → "le-bucket",
      RawTreeServiceImpl.folderConfigKey → "le-file"
    )
    private val configuration = Configuration.from(configData)
    Mockito.when(s3.getLastModified(Matchers.anyString(), Matchers.anyString())) thenReturn None
    val asService = new AdminSectionServiceImpl(configuration, s3, Environment.simple(), mock[SdpSectionDataService], TestExecutionContext.executionContext)
  }

  "AdminSectionService" should {

    "call save when updating Channel" in new Fixture {
      implicit val env = Live

      // given
      val root = emptyWithId(0)
      private val json = Json.toJson(root)(channelWrites).toString
      when(s3.getLastModified(Matchers.anyString(), Matchers.anyString())) thenReturn Some(Instant.now())
      when(s3.getWithLastModified(Matchers.anyString(), Matchers.anyString())) thenReturn Some((json,Instant.now()))

      // when
      asService.update()
      asService.updateChannel(root, root.copy(config = RawChannelConfiguration()), "le-user")

      // then
      val bucket = configData.getOrElse(RawTreeServiceImpl.bucketConfigKey, "")
      val file = configData.getOrElse(RawTreeServiceImpl.folderConfigKey, "")
      verify(s3).putPrivate(Matchers.eq(bucket), startsWith(file), anyString(), contains("json"))
    }

    "update the [RawChannelConfiguration]" in new Fixture {
      implicit val env = Live

      // given
      val testChannel = emptyWithId(0)
      private val json = Json.toJson(testChannel)(channelWrites).toString
      private val expectedConfig = RawChannelConfiguration(
        header = Some(RawChannelHeader(
          logo = Some("foo")
        ))
      )

      // when
      when(s3.getLastModified(Matchers.anyString(), Matchers.anyString())) thenReturn Some(Instant.now())
      when(s3.getWithLastModified(Matchers.anyString(), Matchers.anyString())) thenReturn Some((json,Instant.now()))
      asService.update()
      asService.updateChannel(
        testChannel,
        testChannel.copy(
          config = expectedConfig
        ),
        "le-user")

      // then
      testChannel.config mustBe expectedConfig
    }

    "update the Seq[RawChannelStage] and RawChannelStageConfiguration" in new Fixture {
      implicit val env = Live

      // given
      val expectedModules = Some(Seq(RawChannelStageCustomModule(
        index = 0, module = "foo", trackingName = None, link = None
      )))
      val expectedStageConfig = Some(RawChannelStageConfiguration(stages = expectedModules))
      
      val testChannel = emptyWithId(0)
      private val json = Json.toJson(testChannel)(channelWrites).toString
      when(s3.getLastModified(Matchers.anyString(), Matchers.anyString())) thenReturn Some(Instant.now())
      when(s3.getWithLastModified(Matchers.anyString(), Matchers.anyString())) thenReturn Some((json,Instant.now()))

      // when
      asService.update()
      asService.updateChannel(testChannel,testChannel.copy(stageConfiguration = expectedStageConfig),"le-user")
      
      // then
      testChannel.stageConfiguration mustBe expectedStageConfig
    }
  }

}
