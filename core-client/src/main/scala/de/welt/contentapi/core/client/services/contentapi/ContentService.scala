package de.welt.contentapi.core.client.services.contentapi

import javax.inject.{Inject, Singleton}

import com.google.inject.ImplementedBy
import com.kenshoo.play.metrics.Metrics
import de.welt.contentapi.core.client.services.CapiExecutionContext
import de.welt.contentapi.core.client.services.http.RequestHeaders
import de.welt.contentapi.core.models.ApiResponse
import play.api.Configuration
import play.api.libs.json.{JsLookupResult, JsResult}
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}

/**
  * Reusable (Playframework) Service for Content By ID against our API provided by WeltN24/underwood (Frank).
  * You need valid [[play.Configuration]] values for this Service:
  *
  * * {{{
  *   welt {
  *     api {
  *       content {
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
@ImplementedBy(classOf[ContentServiceImpl])
trait ContentService {

  /**
    * Get a single Content [[de.welt.contentapi.core.models.ApiContent]] wrapped in a [[ApiResponse]].
    *
    * @param id               Escenic ID of the Content ([[de.welt.contentapi.core.models.ApiContent.id]])
    * @param showRelated      Retrieve the Related Content (related/playlist) of the main Content. Default is `true`
    * @param requestHeaders   Forwarded Request Header of the Caller. Used for signing requests with a unique identifier.
    *                         Track the call throw all Services
    * @param executionContext Play [[scala.concurrent.ExecutionContext]] for [[scala.concurrent.Future]]'s
    */
  def find(id: String, showRelated: Boolean = true)
          (implicit requestHeaders: RequestHeaders = Seq.empty, executionContext: ExecutionContext): Future[ApiResponse]
}

@Singleton
class ContentServiceImpl @Inject()(ws: WSClient, metrics: Metrics, configuration: Configuration, capi: CapiExecutionContext)
  extends AbstractService[ApiResponse](ws, metrics, configuration, "content", capi) with ContentService {

  import de.welt.contentapi.core.models.ApiReads._

  override val jsonValidate: JsLookupResult ⇒ JsResult[ApiResponse] = _.validate[ApiResponse]

  override def find(id: String, showRelated: Boolean = true)
                   (implicit requestHeaders: RequestHeaders = Seq.empty, executionContext: ExecutionContext): Future[ApiResponse] = {

    val parameters = if (showRelated) {
      Seq("show-related" → "true")
    } else {
      Seq.empty
    }
    get(urlArguments = Seq(id), parameters = parameters)
  }
}

