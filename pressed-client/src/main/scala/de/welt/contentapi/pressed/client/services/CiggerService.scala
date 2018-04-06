package de.welt.contentapi.pressed.client.services

import com.google.inject.{ImplementedBy, Inject, Singleton}
import com.kenshoo.play.metrics.Metrics
import de.welt.contentapi.core.client.services.CapiExecutionContext
import de.welt.contentapi.core.client.services.contentapi.AbstractService
import de.welt.contentapi.core.client.services.http.RequestHeaders
import de.welt.contentapi.pressed.models.{ApiPressedContentResponse, PressedReads}
import play.api.Configuration
import play.api.libs.json.{JsLookupResult, JsResult}
import play.api.libs.ws.WSClient

import scala.concurrent.Future

@ImplementedBy(classOf[CiggerServiceImpl])
trait CiggerService {
  /**
    * fetch content by its `id`
    *
    * @param id          ece id
    * @param showRelated whether or not related content should also be fetched (mlt, related, playlists,...)
    * @param doEmbed     whether or not oEmbeds should be resolved (AMP)
    * @param page        optional requested page, if content supports paging
    * @param pageSize    optional page size, if content supports paging
    * @param rh          request headers to be forwarded
    * @return `Future[ApiPressedContentResponse]`
    */
  def byId(id: String,
           showRelated: Boolean = false,
           doEmbed: Boolean = false,
           page: Option[Int] = None,
           pageSize: Option[Int] = None)
          (implicit rh: RequestHeaders): Future[ApiPressedContentResponse]
}

@Singleton
class CiggerServiceImpl @Inject()(ws: WSClient, metrics: Metrics, configuration: Configuration, override implicit val capi: CapiExecutionContext)
  extends AbstractService[ApiPressedContentResponse](ws, metrics, configuration, "cigger", capi) with CiggerService {

  override def jsonValidate: JsLookupResult ⇒ JsResult[ApiPressedContentResponse] =
    _.validate[ApiPressedContentResponse](PressedReads.apiPressedContentResponseReads)

  override def byId(id: String,
                    showRelated: Boolean,
                    doEmbed: Boolean,
                    page: Option[Int],
                    pageSize: Option[Int])(implicit rh: RequestHeaders): Future[ApiPressedContentResponse] = {

    val showRelatedParam = if (showRelated) Some("show-related" → "true") else None
    val embedParam = if (doEmbed) Some("embed" → "true") else None
    val pageParam = page.map("page" → _.toString)
    val pageSizeParam = pageSize.map("page-size" → _.toString)

    val parameters = (showRelatedParam ++ embedParam ++ pageParam ++ pageSizeParam).toSeq

    get(Seq(id), parameters)
  }
}
