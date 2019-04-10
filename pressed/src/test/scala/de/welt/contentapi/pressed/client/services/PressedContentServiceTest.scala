package de.welt.contentapi.pressed.client.services

import com.kenshoo.play.metrics.Metrics
import de.welt.contentapi.TestExecutionContext
import de.welt.contentapi.core.models.{ApiContent, ApiSectionData}
import de.welt.contentapi.pressed.client.converter.{InheritanceCalculator, RawToApiConverter}
import de.welt.contentapi.pressed.models.{ApiPressedContent, ApiPressedContentResponse}
import de.welt.contentapi.raw.client.services.RawTreeService
import de.welt.testing.TestHelper.raw.channel._
import de.welt.testing.TestHelper.raw.configuration._
import org.mockito.Mockito._
import org.scalatest.Matchers._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers._

import scala.concurrent.Future

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

    private val related: Seq[ApiPressedContent] = Seq(
      ApiPressedContent(playlistArticle),
      ApiPressedContent(relatedArticle),
      ApiPressedContent(moreLikeThisArticle))
    val relatedAsOptionSeq: Option[Seq[ApiPressedContent]] = Some(related)
    val eventualApiResponse: Future[ApiPressedContentResponse] = Future.successful {
      ApiPressedContentResponse(
        ApiPressedContent(
          content = apiContent,
          related = relatedAsOptionSeq
        ), "test"
      )
    }

    // Given
    val articleId = "1234567890"
    when(contentService.byId(articleId, true)(Seq.empty))
      .thenReturn(eventualApiResponse)
    when(rawTreeService.root).thenReturn(Some(root))

    // When
    val eventualPressedContent: Future[ApiPressedContent] = pressedContentService.find(articleId, showRelated = true)(Seq.empty)
    val apiPressedContent: ApiPressedContent = pressedContentService.convert(await(eventualPressedContent).content, relatedAsOptionSeq.map(_.map(_.content)))
  }

  // Setup
  val contentService: CiggerService = mock[CiggerService]
  val converter: RawToApiConverter = new RawToApiConverter(new InheritanceCalculator())
  val rawTreeService: RawTreeService = mock[RawTreeService]
  val metricsMock: Metrics = mock[Metrics]
  when(metricsMock.defaultRegistry).thenReturn(new com.codahale.metrics.MetricRegistry())

  val pressedContentService: PressedContentService = new PressedContentServiceImpl(contentService, converter, rawTreeService, metricsMock, TestExecutionContext.executionContext)

  "PressedContentService" must {

    "enrich an ApiResponse based on its home section" in new TestScope {
      // Channel is enriched by finding the homeSection
      apiPressedContent.channel.flatMap(_.section).flatMap(_.href) shouldBe Some(node100.id.path)

      // Config is enriched by finding the homeSection
      apiPressedContent.configuration.flatMap(_.commercial).flatMap(_.pathForAdTag) shouldBe Some("10/100")

      // Content from ApiResponse is present
      apiPressedContent.content.id shouldBe apiContent.id

      // Related from ApiResponse is present
      apiPressedContent.related shouldBe relatedAsOptionSeq
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
