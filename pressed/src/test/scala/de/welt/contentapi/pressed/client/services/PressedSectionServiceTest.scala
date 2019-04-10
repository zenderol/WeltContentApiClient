package de.welt.contentapi.pressed.client.services

import java.time.Instant
import java.time.temporal.ChronoUnit

import de.welt.contentapi.core.client.TestExecutionContext
import de.welt.contentapi.core.client.services.configuration.{Development, Production}
import de.welt.contentapi.core.client.services.exceptions.HttpClientErrorException
import de.welt.contentapi.core.client.services.http.RequestHeaders
import de.welt.contentapi.core.models.ApiReference
import de.welt.contentapi.pressed.client.repository.{PressedDiggerClient, PressedS3Client}
import de.welt.contentapi.pressed.models.{ApiChannel, ApiPressedSection, ApiPressedSectionResponse}
import org.apache.http.HttpStatus
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers._

import scala.concurrent.Future

class PressedSectionServiceTest extends PlaySpec with MockitoSugar with TestExecutionContext {

  implicit val requestHeaders: RequestHeaders = Seq.empty

  private trait TestScope {
    val s3Client: PressedS3Client = mock[PressedS3Client]
    val diggerClient: PressedDiggerClient = mock[PressedDiggerClient]
    val pressService: PressedSectionServiceImpl = new PressedSectionServiceImpl(s3Client, diggerClient, executionContext)

    val pressedSection = ApiPressedSection(channel = Some(ApiChannel(Some(ApiReference(Some("label"), Some("/href/"))))))

    val validS3Response = (ApiPressedSectionResponse(Some(pressedSection), "test"),
      Instant.now.minus(PressedSectionService.DefaultSectionTTLMinutes - 5, ChronoUnit.MINUTES))
    val invalidS3Response = (ApiPressedSectionResponse(Some(pressedSection), "test"),
      Instant.now.minus(PressedSectionService.DefaultSectionTTLMinutes + 5, ChronoUnit.MINUTES))

    val pressedSectionFromDigger: Future[ApiPressedSectionResponse] = Future.successful {
      ApiPressedSectionResponse(source = "test", section = Some(pressedSection))
    }

    val sectionPath = "/foo/"
  }

  "PressedSectionService" should {

    "deliver S3 version if present and within ttl" in new TestScope {

      // Given
      when(s3Client.find(ArgumentMatchers.eq(sectionPath)))
        .thenReturn(Some(validS3Response))
      // Mocking just to prove that the section can only come from S3
      when(diggerClient.findByPath(ArgumentMatchers.eq(sectionPath))(ArgumentMatchers.any()))
        .thenThrow(HttpClientErrorException(HttpStatus.SC_NOT_FOUND, "In this test I don't deliver responses", sectionPath))

      // When
      private val resolvedPressedSection = await(pressService.findByPath(sectionPath))

      // Then
      resolvedPressedSection.section.flatMap(_.channel.flatMap(_.section).flatMap(_.href)) mustBe pressedSection.channel.flatMap(_.section).flatMap(_.href)
    }

    "use the outdated s3 result if digger fails" in new TestScope {

      // given
      when(s3Client.find(anyString())).thenReturn(Some(invalidS3Response))
      when(diggerClient.findByPath(anyString())(any())).thenReturn(Future.failed(new IllegalStateException("boum")))


      // when
      val result = await(new PressedSectionServiceImpl(s3Client, diggerClient, executionContext).findByPath("/preview/test"))

      // then
      verify(diggerClient).findByPath(ArgumentMatchers.eq("/preview/test"))(any())
      verify(s3Client).find(ArgumentMatchers.eq("/preview/test"))

      result mustBe invalidS3Response._1
    }

    "ask Digger if S3 Pressed Section is not present" in new TestScope {
      // Given
      when(s3Client.find(sectionPath))
        .thenReturn(None)
      when(diggerClient.findByPath(ArgumentMatchers.eq(sectionPath))(ArgumentMatchers.any()))
        .thenReturn(pressedSectionFromDigger)

      // When
      await(pressService.findByPath(sectionPath))

      // Then
      verify(diggerClient, atLeastOnce).findByPath(ArgumentMatchers.eq(sectionPath))(ArgumentMatchers.any())
    }

    "ask Digger if S3 Pressed Section is too old" in new TestScope {
      // Given
      when(s3Client.find(sectionPath))
        .thenReturn(Some(invalidS3Response))
      when(diggerClient.findByPath(ArgumentMatchers.eq(sectionPath))(ArgumentMatchers.any()))
        .thenReturn(pressedSectionFromDigger)

      // When
      await(pressService.findByPath(sectionPath))

      // Then
      verify(diggerClient, atLeastOnce).findByPath(ArgumentMatchers.eq(sectionPath))(ArgumentMatchers.any())
    }

    "don't ask S3 if Env == Preview" in new TestScope {
      // Given
      when(diggerClient.findByPath(ArgumentMatchers.eq(sectionPath))(ArgumentMatchers.any()))
        .thenReturn(pressedSectionFromDigger)

      // When
      await(pressService.execute(sectionPath, "preview", Production))

      // Then
      verify(s3Client, never).find(sectionPath)
    }

    "don't ask S3 if Mode == Dev" in new TestScope {
      // Given
      when(diggerClient.findByPath(ArgumentMatchers.eq(sectionPath))(ArgumentMatchers.any()))
        .thenReturn(pressedSectionFromDigger)

      // When
      await(pressService.execute(sectionPath, "local", Development))

      // Then
      verify(s3Client, never).find(sectionPath)
    }
  }

}
