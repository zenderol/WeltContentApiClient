package de.welt.contentapi.client.services.contentapi

import de.welt.contentapi.client.services.configuration.ServiceConfiguration
import de.welt.contentapi.client.services.exceptions.{NotFoundException, ServerError}
import de.welt.contentapi.core.traits.Loggable
import play.api.libs.json.{JsError, JsLookupResult, JsResult, JsSuccess}
import play.api.libs.ws.{WSAuthScheme, WSClient, WSResponse}

import scala.concurrent.{ExecutionContext, Future}

trait AbstractService[T] extends Loggable {

  /** these need to be provided by the implementing services */
  val ws: WSClient
  //  val metrics: Metrics

  /**
    * This must provide a [[ServiceConfiguration]]. It will be used to
    * configure the REST request.
    *
    * @return a [[ServiceConfiguration]]
    */
  def config: ServiceConfiguration

  /**
    * This must provide a function that maps a [[JsLookupResult]] to a [[JsResult]].
    * Overriding this outside this trait is required, since the macro-mechanism that
    * takes care of the JSON parsing does not work with generic types `T`
    *
    * @return a [[JsResult]]
    */
  def jsonValidate: JsLookupResult => JsResult[T]

  def get(forwardHeaders: Seq[(String, String)], parameters: Seq[(String, String)], id: String*)(implicit executionContext: ExecutionContext): Future[T] = {

    def parseJson(json: JsLookupResult): T = jsonValidate(json) match {
      case JsSuccess(apiResponse, path) => apiResponse
      case err@JsError(_) => throw new IllegalStateException(err.toString)
    }

    //    def initializeMetricsContext(name: String): Timer.Context = {
    //      metrics.defaultRegistry.timer(MetricRegistry.name(s"funkotron.$name", "requestTimer")).time()
    //    }

    //    val context = initializeMetricsContext(config.serviceName)

    val url: String = config.host + config.endpoint.format(id: _*)

    val request: Future[WSResponse] = ws.url(url)
      .withQueryString(parameters: _*)
      .withHeaders(forwardHeaders: _*)
      .withAuth(config.username, config.password, WSAuthScheme.BASIC).get()

    request.map { response =>

      //      context.stop()

      response.status match {
        case 200 => parseJson(response.json.result)
        case 404 => throw new NotFoundException(url)
        case status@_ => throw new ServerError(s"Server responded with Status '$status' while requesting '$url'.")
      }
    }
  }
}
