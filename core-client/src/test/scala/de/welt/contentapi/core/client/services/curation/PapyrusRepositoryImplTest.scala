package de.welt.contentapi.core.client.services.curation

import java.util.concurrent.TimeUnit

import akka.util.Timeout
import com.kenshoo.play.metrics.Metrics
import de.welt.contentapi.core.client.repository.PapyrusRepositoryImpl
import org.mockito.Matchers
import org.mockito.Matchers.anyString
import org.mockito.Mockito.{verify, when}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSAuthScheme, WSClient, WSRequest, WSResponse}

import scala.concurrent.{Await, ExecutionContext, Future}

class PapyrusRepositoryImplTest extends PlaySpec with MockitoSugar {

  trait TestScope {
    val mockWS = mock[WSClient]
    val metricsMock: Metrics = mock[Metrics]
    when(metricsMock.defaultRegistry).thenReturn(new com.codahale.metrics.MetricRegistry())

    val configuration = Configuration("welt.api.papyrus" → Map(
      "apiKey" → "foo",
      "host" → "http://some.host.name",
      "endpoint" → "/curation/%s/"
    ))

    val papyrusService = PapyrusRepositoryImpl(mockWS, metricsMock, configuration)

    val mockRequest = mock[WSRequest]
    when(mockRequest.withHeaders(Matchers.anyVararg[(String, String)])).thenReturn(mockRequest)
    when(mockRequest.withQueryString(Matchers.anyVararg[(String, String)])).thenReturn(mockRequest)
    when(mockRequest.withAuth(anyString, anyString, Matchers.eq(WSAuthScheme.BASIC))).thenReturn(mockRequest)
    val responseMock = mock[WSResponse]

    implicit val ec = ExecutionContext.global

    when(mockRequest.get()).thenReturn(Future {
      responseMock
    })
  }

  "PapyrusService" should {

    "build the correct url when configured properly" in new TestScope {
      when(mockWS.url(anyString)).thenReturn(mockRequest)
      papyrusService.getByName("icon")(ec = ec)
      verify(mockWS).url("http://some.host.name/curation/icon/")
    }

    "return a CuratedSection if request succeeds" in new TestScope {
      // Given
      when(mockWS.url(anyString)).thenReturn(mockRequest)

      val articleId = "159909527"
      val stageId = "Stage ID"
      val title = "Optional Title"
      val json: JsValue = Json.parse(
        s"""[
           |  {
           |    "id": "$stageId",
           |    "articles": [
           |      {
           |        "id": "$articleId"
           |      }
           |    ],
           |    "title": "$title"
           |  }
           |]""".stripMargin)

      when(responseMock.status).thenReturn(200)
      when(responseMock.json).thenReturn(json)

      // When (using icon because it exists)
      private val eventualSection1 = papyrusService.getByName("icon")(ec = ec)
      private val curatedSection = Await.result(eventualSection1, Timeout(30L, TimeUnit.SECONDS).duration)

      val stage = curatedSection.stages.head

      // Then
      stage.articles.head.id mustBe articleId
      stage.id mustBe stageId
      stage.title mustBe Some(title)
    }

  }

}
