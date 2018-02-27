package de.welt.contentapi.core.client.services.contentapi

import akka.actor.Scheduler
import akka.pattern.CircuitBreaker
import com.codahale.metrics.{Gauge, MetricRegistry, Timer}
import com.kenshoo.play.metrics.Metrics
import de.welt.contentapi.core.client.services.CapiExecutionContext
import de.welt.contentapi.core.client.services.configuration.ServiceConfiguration
import de.welt.contentapi.core.client.services.exceptions.{HttpClientErrorException, HttpRedirectException, HttpServerErrorException}
import de.welt.contentapi.core.client.services.http.RequestHeaders
import de.welt.contentapi.utils.Strings
import play.api.http.{HeaderNames, Status}
import play.api.libs.json.{JsError, JsLookupResult, JsResult, JsSuccess}
import play.api.libs.ws.{WSAuthScheme, WSClient, WSRequest, WSResponse}
import play.api.mvc.Headers
import play.api.{Configuration, Logger}

import scala.concurrent.Future

abstract class AbstractService[T](ws: WSClient,
                                  metrics: Metrics,
                                  configuration: Configuration,
                                  val serviceName: String,
                                  implicit val capi: CapiExecutionContext)
  extends Strings {

  /**
    * This must provide a [[ServiceConfiguration]]. It will be used to
    * configure the REST request.
    *
    * @return a [[ServiceConfiguration]]
    */
  protected[contentapi] val config: ServiceConfiguration = ServiceConfiguration(s"welt.api.$serviceName", configuration)

  /**
    * This must provide a function that maps a [[JsLookupResult]] to a [[JsResult]].
    * Overriding this outside this trait is required, since the macro-mechanism that
    * takes care of the JSON parsing does not work with generic types `T`
    *
    * @return a [[JsResult]]
    */
  def jsonValidate: JsLookupResult => JsResult[T]

  private lazy val breaker: CircuitBreaker = AbstractService.circuitBreaker(capi.actorSystem.scheduler, config, serviceName)

  /**
    * report the breaker state as a gauge to metrics, only if breaker is enabled
    */
  if (config.circuitBreaker.enabled) {
    Logger.info(s"Circuit Breaker enabled for $serviceName")
    metrics.defaultRegistry.register(s"service.$serviceName.circuit_breaker", new Gauge[String]() {
      override def getValue: String = breakerState()
    })
  } else {
    Logger.info(s"Circuit Breaker NOT enabled for $serviceName")
  }

  /**
    * this circuit breaker's current state
    *
    * @return `disabled`, `open`, `closed` or `half-open`
    */
  def breakerState(): String = if (!config.circuitBreaker.enabled) {
    "disabled"
  } else {
    if (breaker.isOpen) "open" else if (breaker.isClosed) "closed" else "half-open"
  }

  /**
    * @param urlArguments            string interpolation arguments for endpoint. e.g. /foo/%s/bar/%s see [[java.lang.String#format}]]
    * @param parameters              URL parameters to be sent with the request
    * @param forwardedRequestHeaders forwarded request headers from the controller e.g. API key
    * @return
    */
  def get(urlArguments: Seq[String] = Nil, parameters: Seq[(String, String)] = Nil)
         (implicit forwardedRequestHeaders: RequestHeaders = Seq.empty): Future[T] = {

    val context = initializeMetricsContext(config.serviceName)

    val url: String = config.host + config.endpoint.format(urlArguments.map(stripWhiteSpaces).filter(_.nonEmpty): _*)

    val filteredParameters = parameters.map { case (k, v) ⇒ k → stripWhiteSpaces(v) }.filter(_._2.nonEmpty)

    val getRequest: WSRequest = ws.url(url)
      .withQueryStringParameters(filteredParameters: _*)
      .addHttpHeaders(forwardHeaders(forwardedRequestHeaders): _*)

    val preparedRequest = config.credentials match {
      case Right(basicAuth) ⇒ getRequest.withAuth(username = basicAuth._1, password = basicAuth._2, WSAuthScheme.BASIC)
      case Left(apiKey) ⇒ getRequest.addHttpHeaders(AbstractService.HeaderApiKey → apiKey)
    }

    Logger.debug(s"HTTP GET to ${preparedRequest.uri}")

    if (config.circuitBreaker.enabled) breaker.withCircuitBreaker(executeAndProcess(preparedRequest, context)) else executeAndProcess(preparedRequest, context)
  }

  private def executeAndProcess(req: WSRequest, context: Timer.Context): Future[T] = req.get()
    .map { response ⇒
      context.stop()
      processResponse(response, req.url)
    }

  /**
    * headers to be forwarded from client to server, e.g. the `X-Unique-Id` or `X-Amzn-Trace-Id`
    *
    * @param maybeHeaders [[Headers]] from the incoming [[play.api.mvc.Request]]
    * @return tuples of type String for headers to be forwarded
    */
  private def forwardHeaders(maybeHeaders: RequestHeaders): RequestHeaders = {
    maybeHeaders.collect {
      case tuple@("X-Unique-Id", _) ⇒ tuple
      case tuple@("X-Amzn-Trace-Id", _) ⇒ tuple
    }
  }

  protected def initializeMetricsContext(name: String): Timer.Context = {
    metrics.defaultRegistry.timer(MetricRegistry.name(s"service.$name", "requestTimer")).time()
  }

  private def processResponse(response: WSResponse, url: String): T = response.status match {
    case Status.OK ⇒ parseJson(response.json.result)
    case status if (300 until 400).contains(status) ⇒ throw HttpRedirectException(url, response.statusText)
    case status if (400 until 500).contains(status) ⇒ throw HttpClientErrorException(status, response.statusText, url, response.header(HeaderNames.CACHE_CONTROL))
    case status ⇒ throw HttpServerErrorException(status, response.statusText, url)
  }

  private def parseJson(json: JsLookupResult): T = jsonValidate(json) match {
    case JsSuccess(value, _) => value
    case err@JsError(_) => throw new IllegalStateException(err.toString)
  }
}

object AbstractService {
  val HeaderApiKey = "x-api-key"

  def circuitBreaker(scheduler: Scheduler, config: ServiceConfiguration, serviceName: String)(implicit ec: CapiExecutionContext) = new CircuitBreaker(
    scheduler,
    maxFailures = config.circuitBreaker.maxFailures,
    callTimeout = config.circuitBreaker.callTimeout,
    resetTimeout = config.circuitBreaker.resetTimeout)
    .withExponentialBackoff(config.circuitBreaker.exponentialBackoff)
    .onOpen(Logger.warn(s"CircuitBreaker [$serviceName] is now open, and will not close for some time"))
    .onClose(Logger.warn(s"CircuitBreaker [$serviceName] is now closed"))
    .onHalfOpen(Logger.warn(s"CircuitBreaker [$serviceName] is now half-open"))
}