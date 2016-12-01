package de.welt.contentapi.core.client.services.contentapi

import javax.inject.{Inject, Singleton}

import com.kenshoo.play.metrics.Metrics
import de.welt.contentapi.core.client.models.ApiContentSearch
import de.welt.contentapi.core.client.services.configuration.ServiceConfiguration
import de.welt.contentapi.core.client.services.http.RequestHeaders
import de.welt.contentapi.core.models.ApiContent
import play.api.Configuration
import play.api.libs.json.{JsLookupResult, JsResult}
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}

sealed trait ContentSearchService {

  val defaultResultSize = 12
  val maxResultSize = 30

  def search(query: ApiContentSearch)
            (implicit requestHeaders: Option[RequestHeaders], executionContext: ExecutionContext): Future[Seq[ApiContent]]


  /**
    * filter invalid values (such as negative or too large)
    *
    * @param maybeLimit the requested limit
    * @return sanitized limit:
    *         the value, if within valid bounds
    *         max allowed is `ContentSearchServiceImpl.maxResultSize`
    *         `ContentSearchServiceImpl.defaultResultSize` if `None` was passed
    */
  def sanitizeLimit(maybeLimit: Option[Int]): Int = {
    maybeLimit.map(l => Math.abs(Math.min(l, maxResultSize))) getOrElse defaultResultSize
  }
}

@Singleton
class ContentSearchServiceImpl @Inject()(override val ws: WSClient,
                                         override val metrics: Metrics,
                                         override val configuration: Configuration)
  extends AbstractService[Seq[ApiContent]] with ContentSearchService {

  import de.welt.contentapi.core.models.ApiReads.apiContentReads

  override val serviceName = "search"
  override val jsonValidate: (JsLookupResult) => JsResult[Seq[ApiContent]] = json => json.validate[Seq[ApiContent]]

  override def search(apiContentSearch: ApiContentSearch)
                     (implicit requestHeaders: Option[RequestHeaders], executionContext: ExecutionContext): Future[Seq[ApiContent]] = {


    get(urlArguments = Nil, parameters = apiContentSearch.getAllParamsUnwrapped)
  }

}
