package de.welt.services.contentapi

import javax.inject.{Inject, Singleton}

import de.welt.models.api.{ApiContent, ApiReads}
import de.welt.services.configuration.{ContentClientConfig, ServiceConfiguration}
import de.welt.traits.Loggable
import play.api.libs.json.{JsLookupResult, JsResult}
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}

trait ContentService {
  protected val serviceName = "content"

  def find(id: String)(implicit executionContext: ExecutionContext): Future[ApiContent]
}

@Singleton
class ContentServiceImpl @Inject()(override val ws: WSClient,
//                                   override val metrics: Metrics,
                                   funkConfig: ContentClientConfig)
  extends AbstractService[ApiContent] with ContentService with Loggable {

  import ApiReads._

  override def jsonValidate: (JsLookupResult) => JsResult[ApiContent] = json => (json \ "content").validate[ApiContent]

  override def config: ServiceConfiguration = funkConfig.getServiceConfig("content")


  override def find(id: String)(implicit executionContext: ExecutionContext): Future[ApiContent] = {
    get(Nil, Nil, id)
  }
}

