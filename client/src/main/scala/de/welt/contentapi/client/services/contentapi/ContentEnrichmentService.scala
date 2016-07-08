package de.welt.contentapi.client.services.contentapi

import javax.inject.{Inject, Singleton}

import de.welt.contentapi.core.models.EnrichedApiResponse
import de.welt.contentapi.core.traits.Loggable

import scala.concurrent.{ExecutionContext, Future}

trait ContentEnrichmentService {
  def find(id: String)(implicit executionContext: ExecutionContext): Future[EnrichedApiResponse]
}

@Singleton
class ContentEnrichmentServiceImpl @Inject()(contentService: ContentService, sectionMetadataService: SectionService)
  extends ContentEnrichmentService with Loggable {

  override def find(id: String)(implicit executionContext: ExecutionContext): Future[EnrichedApiResponse] =
    contentService.find(id).map { response ⇒
      EnrichedApiResponse(sectionMetadataService.enrich(response))
    }
}
