package de.welt.contentapi.pressed.client.repository

import com.google.inject.{ImplementedBy, Inject, Singleton}
import com.kenshoo.play.metrics.Metrics
import de.welt.contentapi.core.client.services.CapiExecutionContext
import de.welt.contentapi.core.client.services.configuration.ServiceConfiguration
import de.welt.contentapi.core.client.services.contentapi.AbstractService
import de.welt.contentapi.core.client.services.http._
import de.welt.contentapi.pressed.models.ApiPressedSectionResponse
import de.welt.contentapi.pressed.models.PressedReads.apiPressedSectionResponseReads
import play.api.libs.ws.{WSClient, WSResponse}

import scala.concurrent.Future
import scala.util.Try

@ImplementedBy(classOf[PressedDiggerClientImpl])
sealed trait PressedDiggerClient {

  /**
    * Get an ApiPressedSection by path from Digger REST Endpoint
    *
    * @param path SectionPath for the Section, e.g. /sport/
    */
  protected[client] def findByPath(path: String)
                                  (implicit requestHeaders: RequestHeaders = Seq.empty): Future[ApiPressedSectionResponse]
}

@Singleton
class PressedDiggerClientImpl @Inject()(ws: WSClient,
                                        metrics: Metrics,
                                        capi: CapiExecutionContext)
  extends AbstractService[ApiPressedSectionResponse](ws, metrics, capi) with PressedDiggerClient {

  import AbstractService.implicitConversions._

  override val validate: WSResponse ⇒ Try[ApiPressedSectionResponse] = response ⇒ response.json.result.validate[ApiPressedSectionResponse]

  override def config: ServiceConfiguration = ServiceConfiguration("digger")

  override protected[client] def findByPath(path: String)
                                           (implicit requestHeaders: RequestHeaders = Seq.empty): Future[ApiPressedSectionResponse] = {
    execute(urlArguments = Seq(path))
  }
}
