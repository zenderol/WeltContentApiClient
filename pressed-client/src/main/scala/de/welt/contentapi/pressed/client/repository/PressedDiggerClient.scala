package de.welt.contentapi.pressed.client.repository

import javax.inject.Inject

import com.kenshoo.play.metrics.Metrics
import de.welt.contentapi.core.client.services.contentapi.AbstractService
import de.welt.contentapi.core.client.services.http._
import de.welt.contentapi.pressed.models.ApiPressedSectionResponse
import de.welt.contentapi.pressed.models.PressedReads.apiPressedSectionResponseReads
import de.welt.contentapi.utils.Env.{Env, Live}
import play.api.Configuration
import play.api.libs.json.{JsLookupResult, JsResult}
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}

sealed trait PressedDiggerClient {

  /**
    * Get an ApiPressedSection by path from Digger REST Endpoint
    *
    * @param path SectionPath for the Section, e.g. /sport/
    * @param env  Live/Preview, default = Live
    */
  protected[client] def findByPath(path: String, env: Env = Live)
                (implicit requestHeaders: RequestHeaders = Seq.empty, executionContext: ExecutionContext): Future[ApiPressedSectionResponse]
}

case class PressedDiggerClientImpl @Inject()(override val ws: WSClient,
                                             override val metrics: Metrics,
                                             override val configuration: Configuration)
  extends AbstractService[ApiPressedSectionResponse] with PressedDiggerClient {

  override val serviceName = "digger"
  override val jsonValidate: (JsLookupResult) ⇒ JsResult[ApiPressedSectionResponse] = jsLookupResult ⇒
    jsLookupResult.validate[ApiPressedSectionResponse](apiPressedSectionResponseReads)

  override protected[client] def findByPath(path: String, env: Env = Live)
                         (implicit requestHeaders: RequestHeaders = Seq.empty, executionContext: ExecutionContext): Future[ApiPressedSectionResponse] = {
    get(urlArguments = Seq(env.toString, path), parameters = Nil)
  }

}
