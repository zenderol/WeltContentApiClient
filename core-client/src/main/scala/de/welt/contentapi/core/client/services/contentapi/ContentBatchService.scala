package de.welt.contentapi.core.client.services.contentapi

import javax.inject.{Inject, Singleton}

import com.google.inject.ImplementedBy
import com.kenshoo.play.metrics.Metrics
import de.welt.contentapi.core.client.services.CapiExecutionContext
import de.welt.contentapi.core.client.services.http.RequestHeaders
import de.welt.contentapi.core.models.{ApiBatchResponse, ApiBatchResult}
import play.api.Configuration
import play.api.libs.json.{JsLookupResult, JsResult}
import play.api.libs.ws.WSClient

import scala.concurrent.Future

/**
  * Reusable (Playframework) Service for batch requesting Content By ID against our API provided by Frank.
  * You need valid [[play.Configuration]] values for this Service:
  *
  * * {{{
  *   welt {
  *     api {
  *       content-batch {
  *         host: "absolute url to the Content API"
  *         endpoint: "relative content endpoint added to the host"
  *         credentials: {
  *           username: "BA username"
  *           password: "BA password"
  *         }
  *       }
  *     }
  *   }
  * }}}
  */
@ImplementedBy(classOf[ContentBatchServiceImpl])
trait ContentBatchService {

  /**
    * Get all requested single Contents [[de.welt.contentapi.core.models.ApiContent]] wrapped in a [[ApiBatchResponse]].
    *
    * @param ids            Escenic IDs of the Content ([[de.welt.contentapi.core.models.ApiContent.id]])
    * @param requestHeaders Forwarded Request Header of the Caller. Used for signing requests with a unique identifier.
    *                       Track the call throw all Services
    */
  def getIds(ids: Seq[String])(implicit requestHeaders: RequestHeaders = Seq.empty): Future[ApiBatchResult]
}

@Singleton
class ContentBatchServiceImpl @Inject()(ws: WSClient, metrics: Metrics, configuration: Configuration, override implicit val capi: CapiExecutionContext)
  extends AbstractService[ApiBatchResult](ws, metrics, configuration, "content-batch", capi) with ContentBatchService {

  import de.welt.contentapi.core.models.ApiReads._

  override val jsonValidate: JsLookupResult ⇒ JsResult[ApiBatchResult] = _.validate[ApiBatchResponse].map(_.response)

  override def getIds(ids: Seq[String])(implicit requestHeaders: RequestHeaders = Seq.empty): Future[ApiBatchResult] = {
    if (ids.isEmpty) {
      Future.successful(ApiBatchResult(Nil))
    } else {
      get(urlArguments = Seq(ids.mkString(",")))
    }
  }
}

