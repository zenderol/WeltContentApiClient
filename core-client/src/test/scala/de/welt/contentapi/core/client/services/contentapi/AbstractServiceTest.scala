package de.welt.contentapi.core.client.services.contentapi

import com.codahale.metrics.Timer.Context
import com.kenshoo.play.metrics.Metrics
import de.welt.contentapi.core.client.services.exceptions.{HttpClientErrorException, HttpServerErrorException}
import de.welt.contentapi.core.client.services.http.RequestHeaders
import org.mockito.Matchers.anyString
import org.mockito.Mockito.{verify, when}
import org.mockito.{Matchers, Mockito}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import play.api.http.Status
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsLookupResult, JsResult, JsString}
import play.api.libs.ws.{WSAuthScheme, WSClient, WSRequest, WSResponse}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class AbstractServiceTest extends PlaySpec
  with MockitoSugar with Status {

  trait TestScope {
    val mockWsClient = mock[WSClient]
    val mockRequest = mock[WSRequest]
    val responseMock = mock[WSResponse]
    val metricsMock = mock[Metrics]
    val mockTimerContext = mock[Context]

    when(mockRequest.withHeaders(Matchers.anyVararg[(String, String)])).thenReturn(mockRequest)
    when(mockRequest.withQueryString(Matchers.anyVararg[(String, String)])).thenReturn(mockRequest)
    when(mockRequest.withAuth(anyString, anyString, Matchers.eq(WSAuthScheme.BASIC))).thenReturn(mockRequest)

    when(mockRequest.get()).thenReturn(Future {
      responseMock
    })

    when(metricsMock.defaultRegistry).thenReturn(new com.codahale.metrics.MetricRegistry())
    when(mockWsClient.url(anyString)).thenReturn(mockRequest)

    class TestService extends AbstractService[String] {
      override val serviceName: String = "test"
      override val configuration: Configuration = TestService.configuration
      override val metrics: Metrics = metricsMock
      override val jsonValidate: (JsLookupResult) => JsResult[String] = json => json.validate[String]
      override val ws: WSClient = mockWsClient

      override protected def initializeMetricsContext(name: String): Context = mockTimerContext
    }

  }

  object TestService {
    val configuration = Configuration("welt.api.test" → Map(
      "host" → "http://www.example.com",
      "endpoint" → "/test/%s",
      "credentials.username" → "user",
      "credentials.password" → "pass"
    ))
//    val config = ServiceConfiguration("test", configuration)
  }

  "AbstractService" should {

    "call the endpoint" in new TestScope {
      new TestService().get(Seq("fake-id"), Seq.empty)
      verify(mockWsClient).url("http://www.example.com/test/fake-id")
    }

    "forward the X-Unique-Id header" in new TestScope {
      val headers = Seq(("X-Unique-Id", "0xdeadbeef"))
      new TestService().get(Seq("fake-id"), Seq.empty)(Some(headers), defaultContext)
      verify(mockRequest).withHeaders(("X-Unique-Id", "0xdeadbeef"))
    }

    "forward the auth data" in new TestScope {
      private val service = new TestService()
      service.get(Seq("fake-id"), Seq("foo" -> "bar"))
      verify(mockRequest).withAuth(service.config.username, service.config.password, WSAuthScheme.BASIC)
    }

    "forward the query string data" in new TestScope {
      new TestService().get(Seq("fake-id"), Seq("foo" -> "bar"))
      verify(mockRequest).withQueryString(("foo", "bar"))
    }

    "forward the headers" in new TestScope {
      implicit val requestHeaders: Option[RequestHeaders] = Some(Seq("X-Unique-Id" -> "bar"))
      new TestService().get(Seq("fake-id"), Seq.empty)
      verify(mockRequest).withHeaders(("X-Unique-Id", "bar"))
    }

    "will return the expected result" in new TestScope {
      when(responseMock.status).thenReturn(OK)
      when(responseMock.json).thenReturn(JsString("the result"))

      val result: Future[String] = new TestService().get(Seq(""), Seq.empty)
      val result1 = Await.result(result, 10.second)
      result1 mustBe "the result"
    }

    "will throw a NotFoundException when WS status is 404" in new TestScope {
      when(responseMock.status).thenReturn(NOT_FOUND)

      val result: Future[String] = new TestService().get(Seq("requested-id"), Seq.empty)
      intercept[HttpClientErrorException] {
        Await.result(result, 10.second)
      }
    }

    "will throw a ServerError when WS status is neither 200 nor 404" in new TestScope {
      when(responseMock.status).thenReturn(GATEWAY_TIMEOUT)

      val result: Future[String] = new TestService().get(Seq("requested-id"), Seq.empty)
      intercept[HttpServerErrorException] {
        Await.result(result, 10.second)
      }
    }

    "will invoke metrics" in new TestScope {
      when(responseMock.status).thenReturn(OK)
      when(responseMock.json).thenReturn(JsString(""))

      val result: Future[String] = new TestService().get(Seq(""))
      Await.result(result, 10.second)
      verify(mockTimerContext).stop()
    }

  }

}
