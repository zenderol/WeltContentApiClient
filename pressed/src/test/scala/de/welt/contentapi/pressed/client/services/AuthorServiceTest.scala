package de.welt.contentapi.pressed.client.services

import java.time.Instant

import de.welt.contentapi.TestExecutionContext
import de.welt.contentapi.core.client.services.aws.s3.S3Client
import de.welt.contentapi.core.client.services.configuration.{ApiConfiguration, Production, Test}
import de.welt.contentapi.core.models.ApiContent
import de.welt.contentapi.pressed.models.ApiPressedContent
import org.mockito.Mockito
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec

class AuthorServiceTest extends PlaySpec with MockitoSugar {

  "AuthorService" should {

    trait TestScope {
      val s3 = mock[S3Client]
      val pcs = mock[PressedContentService]
    }

    "not load any data when in Mode.Test" in new TestScope {
      val service = new AuthorServiceImpl(s3, pcs, TestExecutionContext.executionContext)
      service.update(Test)
      Mockito.verify(s3, Mockito.never()).getLastModified("bucket", "file.key")
    }

    "should invoke the s3 backend when not in Mode.Test" in new TestScope {

      val now: Instant = Instant.now
      Mockito.when(s3.getLastModified(ApiConfiguration.aws.s3.author.bucket, ApiConfiguration.aws.s3.author.file)).thenReturn(Some(now))
      Mockito.when(s3.get(ApiConfiguration.aws.s3.author.bucket, ApiConfiguration.aws.s3.author.file)).thenReturn(Some("[]"))

      val service = new AuthorServiceImpl(s3, pcs, TestExecutionContext.executionContext)
      service.update(Production)
      Mockito.verify(s3).getLastModified(ApiConfiguration.aws.s3.author.bucket, ApiConfiguration.aws.s3.author.file)
      Mockito.verify(s3).get(ApiConfiguration.aws.s3.author.bucket, ApiConfiguration.aws.s3.author.file)
      service.lastModified mustBe now
    }

    trait TestScopeWithAuthors extends TestScope {
      val service = new AuthorServiceImpl(s3, pcs, TestExecutionContext.executionContext)

      val freddy = ApiPressedContent(ApiContent("/autor/freddy/", "author", Some("23")))
      val alphonso = ApiPressedContent(ApiContent("/autor/alphonso/", "author", Some("42")))
    }

    "should find author by URL" in new TestScopeWithAuthors {
      service._allAuthors = Seq(freddy, alphonso)
      service.findByWebURL("/autor/freddy/") must contain(freddy)
    }

    "should return None for unknown author by web url" in new TestScopeWithAuthors {
      service._allAuthors = Seq(freddy, alphonso)
      service.findByWebURL("/autor/bobaaaa/") mustBe empty
    }

    "should find author by escenicId" in new TestScopeWithAuthors {
      service._allAuthors = Seq(freddy, alphonso)
      service.findByEceId("23") must contain(freddy)
    }

    "should return None for unknown author by ece id" in new TestScopeWithAuthors {
      service._allAuthors = Seq(freddy, alphonso)
      service.findByEceId("1337") mustBe empty
    }

    "should return all authors" in new TestScopeWithAuthors {
      service._allAuthors = Seq(freddy, alphonso)
      service.allAuthors() must contain inOrder(freddy, alphonso)
    }
  }
}
