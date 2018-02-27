package de.welt.contentapi.pressed.client.services

import java.util.concurrent.TimeUnit

import akka.util.Timeout
import com.kenshoo.play.metrics.Metrics
import de.welt.contentapi.core.client.services.contentapi.ContentService
import de.welt.contentapi.core.models.{ApiContent, ApiResponse, ApiSectionData}
import de.welt.contentapi.pressed.client.converter.{InheritanceCalculator, RawToApiConverter}
import de.welt.contentapi.pressed.models.ApiPressedContent
import de.welt.contentapi.raw.client.services.RawTreeService
import de.welt.testing.TestHelper.raw.channel._
import de.welt.testing.TestHelper.raw.configuration._
import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.scalatest.Matchers._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec

import scala.concurrent.{Await, ExecutionContext, Future}

//noinspection ScalaStyle
class PressedContentServiceTest extends PlaySpec with MockitoSugar {

  trait TestScope {
    /**
      * {{{
      *    (0 - root)
      *       |
      *      (10)
      *       |
      *      (100)
      * }}}
      */
    val node100 = emptyWithIdAndConfig(100, withAds(true))
    val node10 = emptyWithIdAndChildren(10, Seq(node100))
    val root = emptyWithIdAndChildren(0, Seq(node10))

    root.updateParentRelations()

    import de.welt.contentapi.core.models.testImplicits.pathUpdater

    root.updatePaths()

    val apiContent: ApiContent = ApiContent(
      webUrl = s"${node100.id.path}article1234567890/title.html",
      `type` = "news",
      sections = Some(
        ApiSectionData(
          // Note: The home section must be part of the 'rawTree' aka 'home' used in the PressedContentService
          home = Some(node100.id.path)
        )
      )
    )

    val relatedArticle: ApiContent = ApiContent(
      webUrl = s"${node100.id.path}article1337/t-i-t-l-e.html",
      `type` = "news",
      roles = Some(List("related"))
    )

    val moreLikeThisArticle: ApiContent = ApiContent(
      webUrl = s"${node100.id.path}article0815/extremely-optimized-seo-title.html",
      `type` = "news",
      roles = Some(List("more-like-this"))
    )

    val playlistArticle: ApiContent = ApiContent(
      webUrl = s"${node100.id.path}article0123456789/titleRelated.html",
      `type` = "news",
      roles = Some(List("playlist"))
    )

    val relatedAsOptionSeq: Some[List[ApiContent]] = Some(List(playlistArticle, relatedArticle, moreLikeThisArticle))
    val eventualApiResponse: Future[ApiResponse] = Future.successful {
      ApiResponse(
        apiContent,
        related = relatedAsOptionSeq
      )
    }

    // Given
    val articleId = "1234567890"
    when(contentService
      .find(id = articleId, showRelated = true)(Seq.empty, executionContext))
      .thenReturn(eventualApiResponse)
    when(rawTreeService.root(any())).thenReturn(Some(root))

    // When
    val eventualPressedContent: Future[ApiPressedContent] = pressedContentService.find(articleId, showRelated = true)(Seq.empty, executionContext)
    val apiPressedContent: ApiPressedContent = Await.result(eventualPressedContent, Timeout(30L, TimeUnit.SECONDS).duration)
  }

  implicit lazy val executionContext: ExecutionContext = play.api.libs.concurrent.Execution.Implicits.defaultContext

  // Setup
  val contentService: ContentService = mock[ContentService]
  val converter: RawToApiConverter = new RawToApiConverter(new InheritanceCalculator())
  val rawTreeService: RawTreeService = mock[RawTreeService]
  val metricsMock: Metrics = mock[Metrics]
  when(metricsMock.defaultRegistry).thenReturn(new com.codahale.metrics.MetricRegistry())

  val pressedContentService: PressedContentService = new PressedContentServiceImpl(contentService, converter, rawTreeService, metricsMock)

  "PressedContentService" must {

    "enrich an ApiResponse based on its home section" in new TestScope {
      // Channel is enriched by finding the homeSection
      apiPressedContent.channel.flatMap(_.section).flatMap(_.href) shouldBe Some(node100.id.path)

      // Config is enriched by finding the homeSection
      apiPressedContent.configuration.flatMap(_.commercial).flatMap(_.pathForAdTag) shouldBe Some("10/100")

      // Content from ApiResponse is present
      apiPressedContent.content.id shouldBe apiContent.id

      // Related from ApiResponse is present
      apiPressedContent.related shouldBe relatedAsOptionSeq.map(_.map(x ⇒ ApiPressedContent(x)))
    }

    "filter related articles" in new TestScope {
      // Articles of type 'related' are filtered from related field
      apiPressedContent.relatedContent shouldBe List(ApiPressedContent(relatedArticle))

      // Articles of type 'more-like-this' are filtered from related field
      apiPressedContent.relatedMoreLikeThis shouldBe List(ApiPressedContent(moreLikeThisArticle))

      // Articles of type 'playlist' are filtered from related field
      apiPressedContent.relatedPlaylist shouldBe List(ApiPressedContent(playlistArticle))
    }
  }

}
