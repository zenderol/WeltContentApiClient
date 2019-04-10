package de.welt.contentapi.core.client.services.contentapi

import com.google.inject.{ImplementedBy, Inject, Singleton}
import com.kenshoo.play.metrics.Metrics
import de.welt.contentapi.core.client.services.CapiExecutionContext
import de.welt.contentapi.core.client.services.configuration.ServiceConfiguration
import de.welt.contentapi.core.client.services.http.RequestHeaders
import de.welt.contentapi.core.models.{ApiBatchResponse, ApiBatchResult}
import play.api.libs.ws.{WSClient, WSResponse}

import scala.concurrent.Future
import scala.util.Try

/**
  * Reusable Service for batch requesting Content By ID against our API provided by Frank.
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
class ContentBatchServiceImpl @Inject()(ws: WSClient,
                                        metrics: Metrics,
                                        capi: CapiExecutionContext)
extends AbstractService[ApiBatchResult](ws, metrics, capi) with ContentBatchService {

  import de.welt.contentapi.core.models.ApiReads._
  import AbstractService.implicitConversions._

  override val validate: WSResponse ⇒ Try[ApiBatchResult] = _.json.result.validate[ApiBatchResponse].map(_.response)

  override def getIds(ids: Seq[String])(implicit requestHeaders: RequestHeaders = Seq.empty): Future[ApiBatchResult] = {
    if (ids.isEmpty) {
      Future.successful(ApiBatchResult(Nil))
    } else {
      execute(urlArguments = Seq(ids.mkString(",")))
    }
  }

  override val config: ServiceConfiguration = ServiceConfiguration("content-batch")
}

