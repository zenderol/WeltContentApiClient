package de.welt.contentapi.core.client.services.contentapi

import javax.inject.{Inject, Singleton}

import com.kenshoo.play.metrics.Metrics
import de.welt.contentapi.core.client.models.ApiContentSearch
import de.welt.contentapi.core.client.services.http.RequestHeaders
import de.welt.contentapi.core.models.ApiContent
import play.api.Configuration
import play.api.libs.json.{JsLookupResult, JsResult}
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}

/**
  * Reusable (Playframework) Service for Content Search against our API provided by WeltN24/underwood (Frank).
  * You need valid [[play.Configuration]] values for this Service:
  *
  * {{{
  *   welt {
  *     api {
  *       search {
  *         host: "absolute url to the Content API"
  *         endpoint: "relative search endpoint added to the host"
  *         credentials: {
  *           username: "BA username"
  *           password: "BA password"
  *         }
  *       }
  *     }
  *   }
  * }}}
  */
sealed trait ContentSearchService {

  final val defaultResultSize = 12
  final val maxResultSize = 30

  /**
    * Search for a specific query against the content-api. WeltN24/underwood (Frank)
    *
    * @param query            Search Query Builder
    * @param requestHeaders   Forwarded Request Header of the Caller. Used for signing requests with a unique identifier.
    *                         Track the call throw all Services
    * @param executionContext Play [[scala.concurrent.ExecutionContext]] for [[scala.concurrent.Future]]'s
    */
  def search(query: ApiContentSearch)
            (implicit requestHeaders: RequestHeaders = Seq.empty, executionContext: ExecutionContext): Future[Seq[ApiContent]]


  /**
    * filter invalid values (such as negative or too large)
    *
    * @param maybeLimit the requested limit
    * @return sanitized limit:
    *         the value, if within valid bounds
    *         max allowed is [[ContentSearchService.maxResultSize]]
    *         [[ContentSearchService.defaultResultSize]] if [[None]] was passed
    */
  def sanitizeLimit(maybeLimit: Option[Int]): Int = maybeLimit.map(l ⇒ Math.abs(Math.min(l, maxResultSize))) getOrElse defaultResultSize
}

@Singleton
class ContentSearchServiceImpl @Inject()(override val ws: WSClient,
                                         override val metrics: Metrics,
                                         override val configuration: Configuration)
  extends AbstractService[Seq[ApiContent]] with ContentSearchService {

  import de.welt.contentapi.core.models.ApiReads.apiContentReads

  override val serviceName = "search"
  override val jsonValidate: (JsLookupResult) ⇒ JsResult[Seq[ApiContent]] = json ⇒ json.validate[Seq[ApiContent]]

  override def search(apiContentSearch: ApiContentSearch)
                     (implicit requestHeaders: RequestHeaders  = Seq.empty, executionContext: ExecutionContext): Future[Seq[ApiContent]] = {
    super.get(urlArguments = Nil, parameters = apiContentSearch.getAllParamsUnwrapped)
  }

}
