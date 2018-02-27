package de.welt.contentapi.core.client.services.contentapi

import javax.inject.{Inject, Singleton}

import com.google.inject.ImplementedBy
import com.kenshoo.play.metrics.Metrics
import de.welt.contentapi.core.client.models.ApiContentSearch
import de.welt.contentapi.core.client.services.CapiExecutionContext
import de.welt.contentapi.core.client.services.http.RequestHeaders
import de.welt.contentapi.core.models.{ApiContent, ApiSearchResponse}
import play.api.Configuration
import play.api.libs.json.{JsLookupResult, JsResult}
import play.api.libs.ws.WSClient

import scala.concurrent.Future

/**
  * Reusable (Playframework) Service for Content Search against our search API.
  */
@ImplementedBy(classOf[ContentSearchServiceImpl])
sealed trait ContentSearchService {

  final val defaultResultSize = 12
  final val maxResultSize = 200

  /**
    * Search for a specific query against the content-api. WeltN24/underwood (Frank)
    *
    * @param query          Search Query Builder
    * @param requestHeaders Forwarded Request Header of the Caller. Used for signing requests with a unique identifier.
    *                       Track the call throw all Services
    */
  def search(query: ApiContentSearch)
            (implicit requestHeaders: RequestHeaders = Seq.empty): Future[ApiSearchResponse]


  /**
    * Batch get Content for a Seq of EscenicIds
    *
    * @param ids provide one or more EscenicIds to resolve
    */
  def batchGetForId(ids: Seq[String])
                   (implicit requestHeaders: Option[RequestHeaders]): Future[Seq[ApiContent]]

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
class ContentSearchServiceImpl @Inject()(ws: WSClient, metrics: Metrics, configuration: Configuration, override implicit val capi: CapiExecutionContext)
  extends AbstractService[ApiSearchResponse](ws, metrics, configuration,"search", capi) with ContentSearchService {

  import de.welt.contentapi.core.models.ApiReads.apiSearchResponseReads

  override val jsonValidate: (JsLookupResult) ⇒ JsResult[ApiSearchResponse] = json ⇒ (json \ "response").validate[ApiSearchResponse]

  override def search(apiContentSearch: ApiContentSearch)
                     (implicit requestHeaders: RequestHeaders = Seq.empty): Future[ApiSearchResponse] = {
    super.get(parameters = apiContentSearch.getAllParamsUnwrapped)
  }

  override def batchGetForId(ids: Seq[String])(implicit requestHeaders: Option[RequestHeaders]): Future[Seq[ApiContent]] =
    super
      .get(urlArguments = Nil, parameters = Seq("id" → ids.mkString("|")))
      .map(_.results)
}
