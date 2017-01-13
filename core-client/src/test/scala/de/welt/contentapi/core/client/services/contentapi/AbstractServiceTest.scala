package de.welt.contentapi.core.client.services.contentapi

import com.codahale.metrics.Timer.Context
import com.kenshoo.play.metrics.Metrics
import de.welt.contentapi.core.client.models.{ApiContentSearch, MainTypeParam}
import de.welt.contentapi.core.client.services.exceptions.{HttpClientErrorException, HttpRedirectException, HttpServerErrorException}
import de.welt.contentapi.core.client.services.http.RequestHeaders
import org.mockito.Matchers
import org.mockito.Matchers.anyString
import org.mockito.Mockito.{verify, when}
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
  }

  "AbstractService" should {

    "call the endpoint" in new TestScope {
      new TestService().get(Seq("fake-id"), Seq.empty)
      verify(mockWsClient).url("http://www.example.com/test/fake-id")
    }

    "forward the X-Unique-Id header" in new TestScope {
      val headers = Seq(("X-Unique-Id", "0xdeadbeef"))
      new TestService().get(Seq("fake-id"), Seq.empty)(headers, defaultContext)
      verify(mockRequest).withHeaders(("X-Unique-Id", "0xdeadbeef"))
    }

    "forward the auth data" in new TestScope {
      private val service = new TestService()
      service.get(Seq("fake-id"), Seq("foo" → "bar"))
      verify(mockRequest).withAuth(service.config.username, service.config.password, WSAuthScheme.BASIC)
    }

    "forward the query string data" in new TestScope {
      new TestService().get(Seq("fake-id"), Seq("foo" → "bar"))
      verify(mockRequest).withQueryString(("foo", "bar"))
    }

    "forward the headers" in new TestScope {
      implicit val requestHeaders: RequestHeaders = Seq("X-Unique-Id" → "bar")
      new TestService().get(Seq("fake-id"), Seq.empty)
      verify(mockRequest).withHeaders(("X-Unique-Id", "bar"))
    }

    "strip whitespaces and newline from the parameter" in new TestScope {
      new TestService().get(Seq("with-whitespace \n"))
      verify(mockWsClient).url("http://www.example.com/test/with-whitespace")
    }

    "strip nonbreaking whitespace from parameters" in new TestScope {
      new TestService().get(Seq("strange-whitespaces"), ApiContentSearch(MainTypeParam(List("\u00A0", " ", "\t", "\n"))).getAllParamsUnwrapped)
      verify(mockRequest).withQueryString()
    }

    "not strip valid parameters" in new TestScope {
      val parameters = ApiContentSearch(MainTypeParam(List("param1", "\u00A0param2\u00A0", "\u00A0"))).getAllParamsUnwrapped
      new TestService().get(Seq("strange-whitespaces"), parameters)
      verify(mockRequest).withQueryString("type" → Seq("param1", "param2").mkString(MainTypeParam().operator))
    }

    "strip empty elements from the query string" in new TestScope {
      new TestService().get(Seq("x"), Seq("spaces" → " \n", "trim" → "   value   "))
      verify(mockRequest).withQueryString(("trim", "value"))
    }

    "will return the expected result" in new TestScope {
      when(responseMock.status).thenReturn(OK)
      when(responseMock.json).thenReturn(JsString("the result"))

      val result: Future[String] = new TestService().get(Seq("x"), Seq.empty)
      val result1 = Await.result(result, 10.second)
      result1 mustBe "the result"
    }

    "will throw a RedirectErrorException when WS status is 301" in new TestScope {
      when(responseMock.status).thenReturn(MOVED_PERMANENTLY)

      val result: Future[String] = new TestService().get(Seq("requested-id"), Seq.empty)
      the[HttpRedirectException] thrownBy {
        Await.result(result, 10.second)
      } must matchPattern {
        case HttpRedirectException(_, _, _) ⇒
      }
    }

    "will throw a ClientErrorException when WS status is 404" in new TestScope {
      when(responseMock.status).thenReturn(NOT_FOUND)

      val result: Future[String] = new TestService().get(Seq("requested-id"), Seq.empty)
      the[HttpClientErrorException] thrownBy {
        Await.result(result, 10.second)
      } must matchPattern {
        case HttpClientErrorException(NOT_FOUND, _, _) ⇒
      }
    }

    "will throw a ServerErrorException when WS status is 504" in new TestScope {
      when(responseMock.status).thenReturn(GATEWAY_TIMEOUT)

      val result: Future[String] = new TestService().get(Seq("requested-id"), Seq.empty)
      the[HttpServerErrorException] thrownBy {
        Await.result(result, 10.second)
      } must matchPattern {
        case HttpServerErrorException(GATEWAY_TIMEOUT, _, _) ⇒
      }
    }

    "will invoke metrics" in new TestScope {
      when(responseMock.status).thenReturn(OK)
      when(responseMock.json).thenReturn(JsString(""))

      val result: Future[String] = new TestService().get(Seq("x"))
      Await.result(result, 10.second)
      verify(mockTimerContext).stop()
    }

  }

}
