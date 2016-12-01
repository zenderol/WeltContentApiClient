package de.welt.contentapi.core.client.services.contentapi

import javax.inject.{Inject, Singleton}

import com.kenshoo.play.metrics.Metrics
import de.welt.contentapi.core.client.services.http.RequestHeaders
import de.welt.contentapi.core.models.ApiResponse
import de.welt.contentapi.utils.Loggable
import play.api.Configuration
import play.api.libs.json.{JsLookupResult, JsResult}
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}

trait ContentService {

  def find(id: String, showRelated: Boolean = true)
          (implicit requestHeaders: Option[RequestHeaders], executionContext: ExecutionContext): Future[ApiResponse]
}

@Singleton
class ContentServiceImpl @Inject()(override val ws: WSClient,
                                   override val metrics: Metrics,
                                   override val configuration: Configuration)
  extends AbstractService[ApiResponse] with ContentService with Loggable {

  override val serviceName = "content"

  import de.welt.contentapi.core.models.ApiReads._

  override val jsonValidate: JsLookupResult ⇒ JsResult[ApiResponse] = _.validate[ApiResponse]

  override def find(id: String, showRelated: Boolean)
                   (implicit requestHeaders: Option[RequestHeaders], executionContext: ExecutionContext): Future[ApiResponse] = {

    val parameters = if (showRelated) {
      Seq("show-related" → "true")
    } else {
      Seq.empty
    }
    get(urlArguments = Seq(id), parameters = parameters)
  }
}

