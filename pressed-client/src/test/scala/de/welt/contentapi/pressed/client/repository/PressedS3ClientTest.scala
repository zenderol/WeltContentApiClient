package de.welt.contentapi.pressed.client.repository

import java.time.Instant

import de.welt.contentapi.core.client.services.s3.S3Client
import de.welt.contentapi.core.models.ApiReference
import de.welt.contentapi.pressed.models.{ApiChannel, ApiPressedSection}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.Matchers._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import play.api.libs.json.Json

class PressedS3ClientTest extends PlaySpec with MockitoSugar {

  trait TestScope {
    val path = "/foo/bar/"
    val s3Client: S3Client = mock[S3Client]
    val configuration = Configuration.from(Map(
      "welt.aws.s3.pressed.bucket" → "bucket/for/pressed",
      "welt.aws.s3.pressed.file" → "filename.json"
    ))
    val pressedS3Client: PressedS3Client = PressedS3ClientImpl(s3Client, configuration)

    val simpleSection = ApiPressedSection(
      channel = Some(
        ApiChannel(
          section = Some(ApiReference(
            label = Some("label")
          ))
        )
      )
    )

    import de.welt.contentapi.pressed.models.PressedWrites.apiPressedSectionWrites

    val simpleSectionJson: String = Json.toJson(simpleSection).toString()
  }

  "PressedS3Client" must {

    "deliver tuple of content and s3 lastMod " in new TestScope {
      // Given
      private val s3LastMod: Instant = Instant.now
      val s3Response = (simpleSectionJson, s3LastMod)

      when(s3Client.getWithLastModified(any(), any())).thenReturn(Some(s3Response))

      // When
      private val maybeTuple = pressedS3Client.find(path)

      // Then
      // compare the label, because equals method is not implemented
      maybeTuple.map(_._1).flatMap(_.channel).flatMap(_.section).flatMap(_.label) shouldBe simpleSection.channel.flatMap(_.section).flatMap(_.label)
      // lastMod should be present
      maybeTuple.map(_._2) shouldBe Some(s3LastMod)

    }

    "deliver None for AmazonS3Exception(404)" in new TestScope {
      // Given
      private val s3LastMod: Instant = Instant.now
      val s3Response = (simpleSectionJson, s3LastMod)

      when(s3Client.getWithLastModified(any(), any())).thenReturn(None)

      // When -> Then
      pressedS3Client.find(path) shouldBe empty
    }
  }


}
