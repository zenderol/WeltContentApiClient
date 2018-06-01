package de.welt.contentapi.raw.client.services

import de.welt.contentapi.TestExecutionContext
import de.welt.contentapi.core.client.services.s3.S3Client
import de.welt.contentapi.utils.Env.Live
import org.mockito.{Matchers, Mockito}
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.{Configuration, Environment, Mode}

class RawTreeServiceTest extends PlaySpec with MockitoSugar {

  "Configuration" must {

    val expectedBucket = "foo-bucket"
    val expectedFolder = "foo-folder"
    val expectedMode = "foo-mode"
    val expectedPlayMode = "prod"


    sealed trait ConfigurationScope {
      val s3Client: S3Client = mock[S3Client]

      Mockito.when(s3Client.getLastModified(Matchers.anyString(), Matchers.anyString())) thenReturn None
      Mockito.when(s3Client.getWithLastModified(Matchers.anyString(), Matchers.anyString())) thenReturn None
    }

    "use 'mode' from config file" in new ConfigurationScope {
      // given
      val config = Configuration(
        RawTreeServiceImpl.bucketConfigKey → expectedBucket,
        RawTreeServiceImpl.folderConfigKey → expectedFolder,
        RawTreeServiceImpl.modeConfigKey → expectedMode
      )
      val service = new RawTreeServiceImpl(s3Client, config = config, environment = Environment.simple(mode = Mode.Prod), TestExecutionContext.executionContext)

      // when
      service.root(Live)

      // then
      Mockito.verify(s3Client).getLastModified(expectedBucket, s"$expectedFolder/$expectedMode/Live/config.json")
    }

    "use 'play.Mode' as fallback" in new ConfigurationScope {
      // given
      val config = Configuration(
        RawTreeServiceImpl.bucketConfigKey → expectedBucket,
        RawTreeServiceImpl.folderConfigKey → expectedFolder
      )
      val service = new RawTreeServiceImpl(s3Client, config = config, environment = Environment.simple(mode = Mode.Prod), TestExecutionContext.executionContext)

      // when
      service.root(Live)

      // then
      Mockito.verify(s3Client).getLastModified(expectedBucket, s"$expectedFolder/$expectedPlayMode/Live/config.json")
    }

    "don't access s3 when in Mode.Test" in new ConfigurationScope {
      // given
      val config = Configuration(
        RawTreeServiceImpl.bucketConfigKey → expectedBucket,
        RawTreeServiceImpl.folderConfigKey → expectedFolder
      )
      val service = new RawTreeServiceImpl(s3Client, config = config, environment = Environment.simple(mode = Mode.Test), TestExecutionContext.executionContext)

      // when
      service.root(Live)

      // then
      Mockito.verify(s3Client, Mockito.never()).getLastModified(Matchers.anyString(), Matchers.anyString())
    }

  }

}
