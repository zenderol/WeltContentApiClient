package de.welt.contentapi.raw.client.services

import de.welt.contentapi.TestExecutionContext
import de.welt.contentapi.core.client.services.aws.s3.S3Client
import de.welt.contentapi.core.client.services.configuration.ApiConfiguration
import org.mockito.{ArgumentMatchers, Mockito}
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec

class RawTreeServiceTest extends PlaySpec with MockitoSugar {

  "Configuration" must {

    sealed trait ConfigurationScope {
      val s3Client: S3Client = mock[S3Client]

      Mockito.when(s3Client.getLastModified(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())) thenReturn None
      Mockito.when(s3Client.getWithLastModified(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())) thenReturn None
    }

    "invoke the backend when manually triggering the update" in new ConfigurationScope {
      // given
      val service = new RawTreeServiceImpl(s3Client, TestExecutionContext.executionContext)

      // when
      service.update()

      // then
      Mockito.verify(s3Client).getLastModified(ApiConfiguration.aws.s3.raw.bucket, ApiConfiguration.aws.s3.raw.file)
    }

    "don't access s3 when in Mode.Test" in new ConfigurationScope {
      // given
      val service = new RawTreeServiceImpl(s3Client, TestExecutionContext.executionContext)

      // when
      service.root mustBe None

      // then
      Mockito.verify(s3Client, Mockito.never()).getLastModified(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())
    }

  }

}
