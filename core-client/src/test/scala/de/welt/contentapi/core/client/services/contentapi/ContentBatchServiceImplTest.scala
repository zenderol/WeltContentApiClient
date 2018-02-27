package de.welt.contentapi.core.client.services.contentapi

import com.codahale.metrics.Timer.Context
import com.kenshoo.play.metrics.Metrics
import de.welt.contentapi.core.client.TestExecutionContext
import de.welt.contentapi.core.models.{ApiBatchResponse, ApiBatchResult, ApiContent}
import org.mockito.Matchers
import org.mockito.Matchers.anyString
import org.mockito.Mockito.{verify, when}
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import play.api.http.Status
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSAuthScheme, WSClient, WSRequest, WSResponse}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class ContentBatchServiceImplTest extends PlaySpec with MockitoSugar with TestExecutionContext {

  trait TestScope {

    val mockWsClient: WSClient = mock[WSClient]
    val mockRequest: WSRequest = mock[WSRequest]
    val responseMock: WSResponse = mock[WSResponse]
    val metricsMock: Metrics = mock[Metrics]
    val mockTimerContext: Context = mock[Context]

    when(mockRequest.withHttpHeaders(Matchers.anyVararg[(String, String)])).thenReturn(mockRequest)
    when(mockRequest.withQueryStringParameters(Matchers.anyVararg[(String, String)])).thenReturn(mockRequest)
    when(mockRequest.addHttpHeaders(Matchers.anyVararg[(String, String)])).thenReturn(mockRequest)
    when(mockRequest.withAuth(anyString, anyString, Matchers.eq(WSAuthScheme.BASIC))).thenReturn(mockRequest)

    when(mockRequest.get()).thenReturn(Future {
      responseMock
    })

    when(metricsMock.defaultRegistry).thenReturn(new com.codahale.metrics.MetricRegistry())
    when(mockWsClient.url(anyString)).thenReturn(mockRequest)
    val hostname = "http://www.example.com"
    val endpoint = "/content/_batch/ids="

    val configuration = Configuration("welt.api.content-batch" → Map(
      "host" → hostname,
      "endpoint" → s"$endpoint%s",
      "credentials.username" → "user",
      "credentials.password" → "pass"
    ))

    val batchService: ContentBatchService = new ContentBatchServiceImpl(mockWsClient, metricsMock, configuration, executionContext)
  }

  /** Happy Path Testing only
    * Error Paths are tested in [[AbstractServiceTest]]
    */
  "ContentBatchService" must {

    "String-format the endpoint by using the ids as QueryParam" in new TestScope {
      batchService.getIds(Seq("123", "234"))
      verify(mockWsClient).url(s"http://www.example.com/content/_batch/ids=123,234")
    }

    "json-validate the response as ApiBatchResult" in new TestScope with Status {
      val ids = Seq("123", "234")
      val apiContents = Seq(
        ApiContent(webUrl = "/123", `type` = "news"),
        ApiContent(webUrl = "/234", `type` = "news")
      )
      val batchResult = ApiBatchResult(apiContents)
      val apiResponse = ApiBatchResponse(batchResult)

      import de.welt.contentapi.core.models.ApiWrites._

      val mockJsResponse: JsValue = Json.toJson(apiResponse)

      when(responseMock.status).thenReturn(OK)
      when(responseMock.json).thenReturn(mockJsResponse)

      Await.result(batchService.getIds(ids), 1.second).results mustBe apiContents
    }

    "not try to request an empty list of ids" in new TestScope {
      Await.result(batchService.getIds(Nil), 1.second).results mustBe Nil
    }

  }


}
