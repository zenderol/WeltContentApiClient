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
import org.scalatest.mock.MockitoSugar
import org.scalatest.words.MustVerb
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.{Await, ExecutionContext, Future}

//noinspection ScalaStyle
class PressedContentServiceTest extends FlatSpec
  with Matchers with MustVerb with MockitoSugar {

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
    val relatedAsOptionSeq: Some[List[ApiContent]] = Some(List(
      ApiContent(
        webUrl = s"${node100.id.path}article0123456789/titleRelated.html",
        `type` = "news"
      )
    ))
    val eventualApiResponse: Future[ApiResponse] = Future.successful {
      ApiResponse(
        apiContent,
        related = relatedAsOptionSeq
      )
    }
  }

  implicit lazy val executionContext: ExecutionContext = play.api.libs.concurrent.Execution.Implicits.defaultContext

  // Setup
  val contentService: ContentService = mock[ContentService]
  val converter: RawToApiConverter = new RawToApiConverter(new InheritanceCalculator())
  val rawTreeService: RawTreeService = mock[RawTreeService]
  val metricsMock: Metrics = mock[Metrics]
  when(metricsMock.defaultRegistry).thenReturn(new com.codahale.metrics.MetricRegistry())

  val pressedContentService: PressedContentService = new PressedContentServiceImpl(contentService, converter, rawTreeService, metricsMock)

  // Test Content


  "PressedContentService" must "enrich an ApiResponse based on its home section" in new TestScope {
    // Given
    when(contentService
      .find(id = "1234567890", showRelated = true)(None, executionContext))
      .thenReturn(eventualApiResponse)
    when(rawTreeService.root(any())).thenReturn(Some(root))

    // When
    val eventualPressedContent: Future[ApiPressedContent] = pressedContentService.find("1234567890", showRelated = true)(None, executionContext)
    val apiPressedContent: ApiPressedContent = Await.result(eventualPressedContent, Timeout(30L, TimeUnit.SECONDS).duration)

    // Then
    // Channel is enriched by finding the homeSection
    apiPressedContent.channel.flatMap(_.section).flatMap(_.href) shouldBe Some(node100.id.path)
    // Config is enriched by finding the homeSection
    apiPressedContent.configuration.flatMap(_.commercial).flatMap(_.pathForAdTag) shouldBe Some("10/100")
    // Content from ApiResponse is present
    apiPressedContent.content.id shouldBe apiContent.id
    // Related from ApiResponse is present
    apiPressedContent.related shouldBe relatedAsOptionSeq.map(_.map(x ⇒ ApiPressedContent(x)))
  }

}
