package de.welt.contentapi.client.services.contentapi

import javax.inject.{Inject, Singleton}

import com.kenshoo.play.metrics.Metrics
import de.welt.contentapi.client.services.configuration.{ContentClientConfig, ServiceConfiguration}
import de.welt.contentapi.core.models.{ApiReads, ApiResponse}
import de.welt.contentapi.core.traits.Loggable
import play.api.libs.json.{JsLookupResult, JsResult}
import play.api.libs.ws.WSClient
import play.api.mvc.Request

import scala.concurrent.{ExecutionContext, Future}

trait ContentService {

  protected val serviceName = "content"

  def find(id: String, showRelated: Boolean = true)
          (implicit request: Request[Any], executionContext: ExecutionContext): Future[ApiResponse]
}

@Singleton
class ContentServiceImpl @Inject()(override val ws: WSClient,
                                   override val metrics: Metrics,
                                   funkConfig: ContentClientConfig)
  extends AbstractService[ApiResponse] with ContentService with Loggable {

  import ApiReads._

  override def jsonValidate: JsLookupResult ⇒ JsResult[ApiResponse] = _.validate[ApiResponse]

  override def config: ServiceConfiguration = funkConfig.getServiceConfig(serviceName)

  override def find(id: String, showRelated: Boolean)
                   (implicit request: Request[Any], executionContext: ExecutionContext): Future[ApiResponse] = {

    val parameters = if (showRelated) {
      Seq("show-related" → "true")
    } else {
      Seq.empty
    }
    get(Seq(id), parameters, Nil)
  }
}

