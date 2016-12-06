package de.welt.contentapi.pressed.client.repository

import javax.inject.Inject

import com.kenshoo.play.metrics.Metrics
import de.welt.contentapi.core.client.services.contentapi.AbstractService
import de.welt.contentapi.core.client.services.http._
import de.welt.contentapi.pressed.models.ApiPressedSection
import de.welt.contentapi.utils.Env.{Env, Live}
import play.api.Configuration
import play.api.libs.json.{JsLookupResult, JsResult}
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}

sealed trait PressedDiggerClient {

  def findByPath(path: String, env: Env = Live)
                (implicit requestHeaders: Option[RequestHeaders], executionContext: ExecutionContext): Future[ApiPressedSection]
}

case class PressedDiggerClientImpl @Inject()(override val ws: WSClient,
                                             override val metrics: Metrics,
                                             override val configuration: Configuration)
  extends AbstractService[ApiPressedSection] with PressedDiggerClient {

  import de.welt.contentapi.pressed.models.PressedReads.apiPressedSectionReads

  override val serviceName = "digger"
  override val jsonValidate: (JsLookupResult) ⇒ JsResult[ApiPressedSection] = jsLookupResult ⇒
    jsLookupResult.validate[ApiPressedSection](apiPressedSectionReads)

  override def findByPath(path: String, env: Env = Live)
                         (implicit requestHeaders: Option[RequestHeaders], executionContext: ExecutionContext): Future[ApiPressedSection] = {
    get(urlArguments = Seq(env.toString, path), parameters = Nil)
  }

}
