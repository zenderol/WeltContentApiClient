package de.welt.contentapi.core.client.services.contentapi

import akka.pattern.CircuitBreakerOpenException
import com.codahale.metrics.Timer.Context
import de.welt.contentapi.core.client.TestExecutionContext
import de.welt.contentapi.core.client.services.exceptions.{HttpClientErrorException, HttpRedirectException, HttpServerErrorException}
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatest.concurrent.Eventually
import org.scalatest.mockito.MockitoSugar
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import play.api.libs.json.{JsLookupResult, JsResult, JsString}
import play.api.test.Helpers

import scala.concurrent.Await
import scala.concurrent.duration._

//noinspection ScalaStyle
class CircuitBreakerSpec extends PlaySpec with MockitoSugar with TestExecutionContext {

  "CircuitBreaker" should {
    "if ENABLED" should {

      trait BreakerEnabled extends AbstractServiceTest.TestScope {

        class TestService extends AbstractService[String](mockWsClient, metricsMock,
          CircuitBreakerSpec.breakerEnabled.configuration, "test", executionContext) {
          override val jsonValidate: (JsLookupResult) => JsResult[String] = json => json.validate[String]

          override protected def initializeMetricsContext(name: String): Context = mockTimerContext
        }

      }

      "execute multiple requests in case of no errors" in new BreakerEnabled {

        when(responseMock.status).thenReturn(Helpers.OK)
        when(responseMock.json).thenReturn(JsString(""))

        val service = new TestService()
        for {_ ← 1 to 100} {
          Await.result(service.get(), 1.second)
        }
        verify(mockRequest, times(100)).get()
      }

      "should reject requests when breaker opens" in new BreakerEnabled {
        val service = new TestService()
        service.breaker.fail()
        assertThrows[CircuitBreakerOpenException] {
          Await.result(service.get(), 1.second)
        }
      }

      "close breaker if request succeeds when half-open" in new BreakerEnabled with Eventually {
        val service = new TestService()
        service.breaker.fail()
        assertThrows[CircuitBreakerOpenException] {
          Await.result(service.get(), 1.second)
        }
        // wait for breaker to half-open
        eventually(timeout(Span(2, Seconds)), interval(Span(25, Millis))) {
          service.breaker.isHalfOpen mustBe true
        }
        service.breaker.succeed()
        // succeeding call should reset the breaker to "closed"
        service.breaker.isClosed mustBe true
        // next call should be OK
        when(responseMock.status).thenReturn(Helpers.OK)
        when(responseMock.json).thenReturn(JsString(""))
        Await.result(service.get(), 1.second)

        verify(mockRequest).get()
      }

      "open breaker if request fails when half-open" in new BreakerEnabled with Eventually {
        val service = new TestService()
        service.breaker.fail()
        assertThrows[CircuitBreakerOpenException] {
          Await.result(service.get(), 1.second)
        }
        // wait for breaker to half-open
        eventually(timeout(Span(2, Seconds)), interval(Span(25, Millis))) {
          service.breaker.isHalfOpen mustBe true
        }
        service.breaker.fail()
        // succeeding call should reset the breaker to "closed"
        service.breaker.isOpen mustBe true
        // next call should fail
        assertThrows[CircuitBreakerOpenException] {
          Await.result(service.get(), 1.second)
        }

        verify(mockRequest, never()).get()
      }

      "2xx errors will not open the breaker" in new BreakerEnabled {

        when(responseMock.status).thenReturn(Helpers.OK)
        when(responseMock.json).thenReturn(JsString("expected-value"))

        val service = new TestService()
        Await.result(service.get(), 1.second) mustBe "expected-value"

        service.breaker.isClosed mustBe true
      }

      "3xx errors will not open the breaker" in new BreakerEnabled {

        when(responseMock.status).thenReturn(Helpers.MOVED_PERMANENTLY)

        val service = new TestService()
        assertThrows[HttpRedirectException] {
          Await.result(service.get(), 1.second)
        }

        service.breaker.isClosed mustBe true
      }

      "4xx errors will not open the breaker" in new BreakerEnabled {
        when(responseMock.status).thenReturn(Helpers.GONE)

        val service = new TestService()
        assertThrows[HttpClientErrorException] {
          Await.result(service.get(), 1.second)
        }

        service.breaker.isClosed mustBe true
      }

      "5xx errors will open the breaker" in new BreakerEnabled {
        when(responseMock.status).thenReturn(Helpers.INTERNAL_SERVER_ERROR)

        val service = new TestService()
        // the first exception is returned and opens the breaker
        assertThrows[HttpServerErrorException] {
          Await.result(service.get(), 1.second)
        }
        // the second request will hit the open breaker
        assertThrows[CircuitBreakerOpenException] {
          Await.result(service.get(), 1.second)
        }
        service.breaker.isOpen mustBe true
      }


    }
    "if DISABLED" should {

      trait BreakerDisabled extends AbstractServiceTest.TestScope {

        class TestService extends AbstractService[String](mockWsClient, metricsMock,
          CircuitBreakerSpec.breakerDisabled.configuration, "test", executionContext) {
          override val jsonValidate: (JsLookupResult) => JsResult[String] = json => json.validate[String]

          override protected def initializeMetricsContext(name: String): Context = mockTimerContext
        }

      }

      "execute multiple requests in case of no errors" in new BreakerDisabled {

        when(responseMock.status).thenReturn(Helpers.OK)
        when(responseMock.json).thenReturn(JsString(""))

        val service = new TestService()
        for {i ← 1 to 100} {
          Await.result(service.get(), 1.second)
        }
        verify(mockRequest, times(100)).get()
      }

      "execute multiple requests even if failures occur" in new BreakerDisabled {
        when(responseMock.status).thenReturn(Helpers.GONE)

        val service = new TestService()

        assertThrows[HttpClientErrorException] {
          Await.result(service.get(), 1.second)
        }
        assertThrows[HttpClientErrorException] {
          Await.result(service.get(), 1.second)
        }

        verify(mockRequest, times(2)).get()
      }

      "2xx errors will return the expected content" in new BreakerDisabled {

        when(responseMock.status).thenReturn(Helpers.OK)
        when(responseMock.json).thenReturn(JsString("expected-value"))

        val service = new TestService()
        Await.result(service.get(), 1.second) mustBe "expected-value"
      }

      "3xx errors will cause redirect exceptions" in new BreakerDisabled {

        when(responseMock.status).thenReturn(Helpers.MOVED_PERMANENTLY)

        val service = new TestService()
        assertThrows[HttpRedirectException] {
          Await.result(service.get(), 1.second)
        }
      }

      "4xx errors will cause client error exceptions" in new BreakerDisabled {
        when(responseMock.status).thenReturn(Helpers.GONE)

        val service = new TestService()
        assertThrows[HttpClientErrorException] {
          Await.result(service.get(), 1.second)
        }
      }

      "5xx errors will cause server error exceptions" in new BreakerDisabled {
        when(responseMock.status).thenReturn(Helpers.INTERNAL_SERVER_ERROR)

        val service = new TestService()
        assertThrows[HttpServerErrorException] {
          Await.result(service.get(), 1.second)
        }

      }

    }
  }

}

object CircuitBreakerSpec {

  object breakerEnabled {
    val configuration = Configuration("welt.api.test" → Map(
      "host" → "http://www.example.com",
      "endpoint" → "/test",
      "apiKey" → "foo",
      "circuitBreaker" → Map(
        "enabled" → true,
        "maxFailures" → 1,
        "callTimeout" → "500 millis",
        "resetTimeout" → "500 millis",
        "exponentialBackoff" → "2 seconds"
      )
    ))
  }

  object breakerDisabled {
    val configuration = Configuration("welt.api.test" → Map(
      "host" → "http://www.example.com",
      "endpoint" → "/test",
      "apiKey" → "foo",
      "circuitBreaker" → Map(
        "enabled" → false
      )
    ))
  }

}