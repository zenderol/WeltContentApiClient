package de.welt.contentapi.pressed.client.services

import java.time.Instant
import java.time.temporal.ChronoUnit

import de.welt.contentapi.core.client.services.exceptions.HttpClientErrorException
import de.welt.contentapi.core.client.services.http.RequestHeaders
import de.welt.contentapi.core.models.ApiReference
import de.welt.contentapi.pressed.client.repository.{PressedDiggerClient, PressedS3Client}
import de.welt.contentapi.pressed.models.{ApiChannel, ApiPressedSection, ApiPressedSectionResponse}
import de.welt.contentapi.utils.Env
import de.welt.contentapi.utils.Env.{Live, Preview}
import org.apache.http.HttpStatus
import org.mockito.Matchers.{any, anyString}
import org.mockito.Mockito._
import org.mockito.{Matchers ⇒ MockitoMatchers}
import org.scalatest.mock.MockitoSugar
import org.scalatest.words.MustVerb
import org.scalatest.{FlatSpec, Matchers}
import play.api.{Configuration, Mode}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

class PressedSectionServiceTest extends FlatSpec
  with Matchers with MustVerb with MockitoSugar {

  implicit val ec = ExecutionContext.global
  implicit val requestHeaders: RequestHeaders = Seq.empty

  trait TestScope {
    val s3Client: PressedS3Client = mock[PressedS3Client]
    val diggerClient: PressedDiggerClient = mock[PressedDiggerClient]
    val pressService: PressedSectionService = new PressedSectionServiceImpl(s3Client, Configuration(), diggerClient)

    val pressedSection = ApiPressedSection(channel = Some(ApiChannel(Some(ApiReference(Some("label"), Some("/href/"))))))

    val validS3Response = (ApiPressedSectionResponse("test", Some(pressedSection)),
      Instant.now.minus(PressedSectionService.DefaultSectionTTLMinutes - 5, ChronoUnit.MINUTES))
    val invalidS3Response = (ApiPressedSectionResponse("test", Some(pressedSection)),
      Instant.now.minus(PressedSectionService.DefaultSectionTTLMinutes + 5, ChronoUnit.MINUTES))

    val pressedSectionFromDigger: Future[ApiPressedSectionResponse] = Future.successful {
      ApiPressedSectionResponse(source = "test", section = Some(pressedSection))
    }

    val sectionPath = "/foo/"
  }

  "PressedSectionService" must "deliver S3 version if present and within ttl" in new TestScope {

    // Given
    when(s3Client.find(sectionPath))
      .thenReturn(Some(validS3Response))
    // Mocking just to prove that the section can only come from S3
    when(diggerClient.findByPath(sectionPath))
      .thenThrow(HttpClientErrorException(HttpStatus.SC_NOT_FOUND, "In this test I don't deliver responses", sectionPath))

    // When
    private val resolvedPressedSection = Await.result(pressService.findByPath(sectionPath), 1.second)

    // Then
    resolvedPressedSection.section.flatMap(_.channel.flatMap(_.section).flatMap(_.href)) shouldBe pressedSection.channel.flatMap(_.section).flatMap(_.href)
  }

  it must "use the outdated s3 result if digger fails" in new TestScope {

    // given
    when(s3Client.find(anyString())).thenReturn(Some(invalidS3Response))
    when(diggerClient.findByPath(anyString(), any())(any(), any())).thenReturn(Future.failed(new IllegalStateException("boum")))


    // when
    val result = Await.result(new PressedSectionServiceImpl(s3Client, Configuration(), diggerClient).findByPath("/preview/test", Live), 1.second)

    // then
    verify(diggerClient).findByPath(MockitoMatchers.eq("/preview/test"), any())(any(), any())
    verify(s3Client).find(MockitoMatchers.eq("/preview/test"))

    result shouldBe invalidS3Response._1
  }

  it must "ask Digger if S3 Pressed Section is not present" in new TestScope {
    // Given
    when(s3Client.find(sectionPath))
      .thenReturn(None)
    when(diggerClient.findByPath(sectionPath, Live))
      .thenReturn(pressedSectionFromDigger)

    // When
    Await.result(awaitable = pressService.findByPath(sectionPath), 1.second)

    // Then
    verify(diggerClient, atLeastOnce).findByPath(sectionPath, Live)
  }

  it must "ask Digger if S3 Pressed Section is too old" in new TestScope {
    // Given
    when(s3Client.find(sectionPath))
      .thenReturn(Some(invalidS3Response))
    when(diggerClient.findByPath(sectionPath, Live))
      .thenReturn(pressedSectionFromDigger)

    // When
    Await.result(awaitable = pressService.findByPath(sectionPath), 1.second)

    // Then
    verify(diggerClient, atLeastOnce).findByPath(sectionPath, Live)
  }

  it must "don't ask S3 if Env == Preview" in new TestScope {
    // Given
    when(diggerClient.findByPath(sectionPath, Preview)).thenReturn(pressedSectionFromDigger)

    // When
    Await.result(awaitable = pressService.findByPath(sectionPath, Env.Preview), 1.second)

    // Then
    verify(s3Client, never).find(sectionPath)
  }

  it must "don't ask S3 if Mode == Dev" in new TestScope {
    // Given
    when(diggerClient.findByPath(sectionPath, Live)).thenReturn(pressedSectionFromDigger)

    // When
    Await.result(awaitable = pressService.findByPath(sectionPath, Live, Mode.Dev), 1.second)

    // Then
    verify(s3Client, never).find(sectionPath)
  }

  it must "pass the (preview) env to the service" in {

    // given
    val client = mock[PressedDiggerClient]
    when(client.findByPath("/preview/test", Preview)).thenReturn(Future.successful(ApiPressedSectionResponse("test", None)))

    // when
    Await.result(new PressedSectionServiceImpl(mock[PressedS3Client], Configuration(), client).findByPath("/preview/test", Preview), 1.second)

    // then
    verify(client).findByPath(MockitoMatchers.eq("/preview/test"), MockitoMatchers.eq(Preview))(any(), any())
  }

}
