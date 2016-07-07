package de.welt.client

import com.sun.javafx.font.Metrics
import de.welt.contentapi.client.services.configuration.ServiceConfiguration
import de.welt.contentapi.client.services.contentapi.AbstractService
import de.welt.contentapi.client.services.exceptions.{NotFoundException, ServerError}
import org.mockito.Matchers
import org.mockito.Matchers.anyString
import org.mockito.Mockito.{verify, when}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.http.Status
import play.api.libs.json.{JsLookupResult, JsResult, JsString}
import play.api.libs.ws.{WSAuthScheme, WSClient, WSRequest, WSResponse}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

class AbstractServiceTest extends PlaySpec
  with MockitoSugar with Status {

  trait TestScope {
    val mockWsClient = mock[WSClient]
    val mockRequest = mock[WSRequest]
    val responseMock = mock[WSResponse]
    val metricsMock = mock[Metrics]

    when(mockRequest.withHeaders(Matchers.anyVararg[(String, String)])).thenReturn(mockRequest)
    when(mockRequest.withQueryString(Matchers.anyVararg[(String, String)])).thenReturn(mockRequest)
    when(mockRequest.withAuth(anyString, anyString, Matchers.eq(WSAuthScheme.BASIC))).thenReturn(mockRequest)

    when(mockRequest.get()).thenReturn(Future {
      responseMock
    })

    when(mockWsClient.url(anyString)).thenReturn(mockRequest)

    class TestService extends AbstractService[String] {
      override def config: ServiceConfiguration = TestService.config

      override def jsonValidate: (JsLookupResult) => JsResult[String] = json => json.validate[String]

      override val ws: WSClient = mockWsClient
    }

  }

  object TestService {
    val config = ServiceConfiguration("test", "http://www.example.com", "/test/%s", "user", "pass")
  }

  "AbstractService" should {

    "call the endpoint" in new TestScope {
      new TestService().get(Seq.empty, Seq.empty, "fake-id")
      verify(mockWsClient).url("http://www.example.com/test/fake-id")
    }

    "forward the X-Unique-Id header" in new TestScope {
      new TestService().get(Seq("X-Unique-Id" -> "0xdeadbeef"), Seq.empty, "fake-id")
      verify(mockRequest).withHeaders(("X-Unique-Id", "0xdeadbeef"))
    }

    "forward the auth data" in new TestScope {
      new TestService().get(Seq("foo" -> "bar"), Seq.empty, "fake-id")
      verify(mockRequest).withAuth(TestService.config.username, TestService.config.password, WSAuthScheme.BASIC)
    }

    "forward the query string data" in new TestScope {
      new TestService().get(Seq.empty, Seq("foo" -> "bar"), "fake-id")
      verify(mockRequest).withQueryString(("foo", "bar"))
    }

    "will return the expected result" in new TestScope {
      when(responseMock.status).thenReturn(OK)
      when(responseMock.json).thenReturn(JsString("the result"))

      val result: Future[String] = new TestService().get(Seq.empty, Seq.empty, "")
      val result1 = Await.result(result, 10.second)
      result1 mustBe "the result"
    }

    "will throw a NotFoundException when WS status is 404" in new TestScope {
      when(responseMock.status).thenReturn(NOT_FOUND)

      val result: Future[String] = new TestService().get(Seq.empty, Seq.empty, "requested-id")
      intercept[NotFoundException] {
        Await.result(result, 10.second)
      }
    }

    "will throw a ServerError when WS status is neither 200 nor 404" in new TestScope {
      when(responseMock.status).thenReturn(GATEWAY_TIMEOUT)

      val result: Future[String] = new TestService().get(Seq.empty, Seq.empty, "requested-id")
      intercept[ServerError] {
        Await.result(result, 10.second)
      }
    }

  }

}
