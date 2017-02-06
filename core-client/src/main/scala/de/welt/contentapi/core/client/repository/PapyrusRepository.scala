package de.welt.contentapi.core.client.repository

import javax.inject.Inject

import com.kenshoo.play.metrics.Metrics
import de.welt.contentapi.core.client.models.{CuratedReads, CuratedSection, CuratedStage}
import de.welt.contentapi.core.client.services.contentapi.AbstractService
import de.welt.contentapi.core.client.services.http.RequestHeaders
import play.api.Configuration
import play.api.libs.json.{JsLookupResult, JsResult}
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}

trait PapyrusRepository extends AbstractService[Seq[CuratedStage]] {
  def getByName(curatedSectionName: String)
               (implicit requestHeaders: RequestHeaders = Nil, ec: ExecutionContext): Future[CuratedSection]
}

/**
  * Implementation of the AbstractService for HTTP Communication with the Papyrus API
  * Needed configuration object is: `papyrus` with values `host`, `endpoint` and `apiKey`
  */
case class PapyrusRepositoryImpl @Inject()(override val ws: WSClient,
                                           override val metrics: Metrics,
                                           override val configuration: Configuration) extends PapyrusRepository {
  override def serviceName: String = "papyrus"

  override def jsonValidate: (JsLookupResult) ⇒ JsResult[Seq[CuratedStage]] = json ⇒ {
    import CuratedReads._
    json.validate[Seq[CuratedStage]]
  }

  /**
    * Ask the Papyrus Api for the curated section by its name
    *
    * @param name of the curated section to be fetched, correlates to the path for the papyrus api.
    * @return a [[CuratedSection]] if it exists with this name
    */
  override def getByName(name: String)
                        (implicit headers: RequestHeaders = Nil, ec: ExecutionContext): Future[CuratedSection] = {
    get(Seq(name))(headers, ec).map(stages ⇒
      CuratedSection(
        stages = stages,
        name = Some(name))
    )
  }
}
