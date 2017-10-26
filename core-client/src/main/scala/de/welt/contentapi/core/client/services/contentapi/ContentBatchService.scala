package de.welt.contentapi.core.client.services.contentapi

import javax.inject.{Inject, Singleton}

import com.kenshoo.play.metrics.Metrics
import de.welt.contentapi.core.client.services.http.RequestHeaders
import de.welt.contentapi.core.models.ApiBatchResponse
import de.welt.contentapi.utils.Loggable
import play.api.Configuration
import play.api.libs.json.{JsLookupResult, JsResult}
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}

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
trait ContentBatchService {

  /**
    * Get all requested single Contents [[de.welt.contentapi.core.models.ApiContent]] wrapped in a [[ApiBatchResponse]].
    *
    * @param ids              Escenic IDs of the Content ([[de.welt.contentapi.core.models.ApiContent.id]])
    * @param requestHeaders   Forwarded Request Header of the Caller. Used for signing requests with a unique identifier.
    *                         Track the call throw all Services
    * @param executionContext Play [[scala.concurrent.ExecutionContext]] for [[scala.concurrent.Future]]'s
    */
  def getIds(ids: Seq[String])
            (implicit requestHeaders: RequestHeaders = Seq.empty, executionContext: ExecutionContext): Future[ApiBatchResponse]
}

@Singleton
class ContentBatchServiceImpl @Inject()(override val ws: WSClient,
                                   override val metrics: Metrics,
                                   override val configuration: Configuration)
  extends AbstractService[ApiBatchResponse] with ContentBatchService with Loggable {

  override val serviceName = "content-batch"

  import de.welt.contentapi.core.models.ApiReads._

  override val jsonValidate: JsLookupResult ⇒ JsResult[ApiBatchResponse] = _.validate[ApiBatchResponse]

  override def getIds(ids: Seq[String])
                     (implicit requestHeaders: RequestHeaders = Seq.empty, executionContext: ExecutionContext): Future[ApiBatchResponse] = {

    get(urlArguments = Seq(ids.mkString(",")))
  }
}

