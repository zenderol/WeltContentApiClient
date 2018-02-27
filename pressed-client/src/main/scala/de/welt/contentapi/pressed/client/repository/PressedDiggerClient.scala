package de.welt.contentapi.pressed.client.repository

import javax.inject.{Inject, Singleton}

import com.google.inject.ImplementedBy
import com.kenshoo.play.metrics.Metrics
import de.welt.contentapi.core.client.services.CapiExecutionContext
import de.welt.contentapi.core.client.services.contentapi.AbstractService
import de.welt.contentapi.core.client.services.http._
import de.welt.contentapi.pressed.models.ApiPressedSectionResponse
import de.welt.contentapi.pressed.models.PressedReads.apiPressedSectionResponseReads
import de.welt.contentapi.utils.Env.{Env, Live}
import play.api.Configuration
import play.api.libs.json.{JsLookupResult, JsResult}
import play.api.libs.ws.WSClient

import scala.concurrent.Future

@ImplementedBy(classOf[PressedDiggerClientImpl])
sealed trait PressedDiggerClient {

  /**
    * Get an ApiPressedSection by path from Digger REST Endpoint
    *
    * @param path SectionPath for the Section, e.g. /sport/
    * @param env  Live/Preview, default = Live
    */
  protected[client] def findByPath(path: String, env: Env = Live)(implicit requestHeaders: RequestHeaders = Seq.empty): Future[ApiPressedSectionResponse]
}

@Singleton
class PressedDiggerClientImpl @Inject()(ws: WSClient, metrics: Metrics, configuration: Configuration, capi: CapiExecutionContext)
  extends AbstractService[ApiPressedSectionResponse](ws, metrics, configuration, "digger", capi) with PressedDiggerClient {

  override val jsonValidate: (JsLookupResult) ⇒ JsResult[ApiPressedSectionResponse] = _.validate[ApiPressedSectionResponse](apiPressedSectionResponseReads)

  override protected[client] def findByPath(path: String, env: Env = Live)
                                           (implicit requestHeaders: RequestHeaders = Seq.empty): Future[ApiPressedSectionResponse] = {
    get(urlArguments = Seq(env.toString, path), parameters = Nil)
  }

}
