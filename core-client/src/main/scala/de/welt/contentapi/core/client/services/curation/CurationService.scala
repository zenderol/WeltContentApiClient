package de.welt.contentapi.core.client.services.curation

import javax.inject.{Inject, Singleton}

import de.welt.contentapi.core.client.models.CuratedSection
import de.welt.contentapi.core.client.repository.PapyrusRepository
import de.welt.contentapi.core.client.services.exceptions.HttpClientErrorException
import de.welt.contentapi.core.client.services.http.RequestHeaders
import de.welt.contentapi.utils.Loggable

import scala.concurrent.{ExecutionContext, Future}

trait CurationService {
  /**
    * Use Case: CMCF for filling a DropDown with the names of all curated stages of a curated section
    *
    * @param name the name of the curated section in papyrus, e.g. "icon" or "frontpage"
    * @return a List of all stage names for a curated section in papyrus.
    */
  def getAllStageNamesBySectionName(name: String)(implicit headers: RequestHeaders = Nil, ec: ExecutionContext): Future[Option[Seq[String]]]

  /**
    * fetch the curated items for a single stage by name within a papyrus curated section
    *
    * @param name The name of the stage, that exists within a curated section in papyrus
    * @return asynchronously an CuratedSection if the provided parameters map to a papyrus stage
    */
  def curatedSectionForName(name: String)(implicit headers: RequestHeaders = Nil, ec: ExecutionContext): Future[Option[CuratedSection]]
}

/**
  * CurationService uses the [[PapyrusRepository]] to resolve curated Stage from Papyrus configured in CMCF
  * It handles the business logic, whereas the PapyrusRepository is a dumb HTTP Adapter to the Backend.
  *
  * @param repo an instance of the PapyrusRepository needed for the HTTP Communication with the Backend
  *
  */
@Singleton
class CurationServiceImpl @Inject()(repo: PapyrusRepository) extends CurationService with Loggable {



  override def getAllStageNamesBySectionName(name: String)(implicit headers: RequestHeaders = Nil, ec: ExecutionContext): Future[Option[Seq[String]]] = {
    val eventualMaybeSection = curatedSectionForName(name)(headers, ec)
    eventualMaybeSection
      .map(x ⇒ x.map(_.allCuratedStageIds))
  }

  override def curatedSectionForName(name: String)(implicit headers: RequestHeaders = Nil, ec: ExecutionContext): Future[Option[CuratedSection]] =
    try {
      repo.getByName(name)(headers, ec).map(x ⇒ Some(x))
    } catch {
      case e: HttpClientErrorException ⇒ log.warn(s"404 for CuratedSection '$name'. Cause: ${e.getStatusPhrase}")
        Future(None)
      case t: Throwable ⇒ log.error(s"Unexpected Error for CuratedSection '$name'", t)
        Future(None)
    }
}
