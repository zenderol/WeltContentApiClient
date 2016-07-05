package de.welt.services.contentapi

import javax.inject.{Inject, Singleton}

import de.welt.models.api.{ApiContent, ApiReads}
import de.welt.services.configuration.{ContentClientConfig, ServiceConfiguration}
import play.api.libs.json.{JsLookupResult, JsResult}
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}

sealed trait ContentSearchService {
  protected val serviceName = "search"

  /**
    * @param path   section path. example: /politik/deutschland/
    * @param `type` content type to search for. example: article, gallery, video
    * @param flags  filters, such as HIGHLIGHT or PREMIUM
    * @param limit  limit the resultSize
    */
  def search(path: Option[String], `type`: Option[String] = None, maybeSubType: Option[String] = None, flags: Set[String] = Set.empty, limit: Option[Int] = None)
            (implicit executionContext: ExecutionContext): Future[Seq[EnrichedApiContent]]
}

@Singleton
class ContentSearchServiceImpl @Inject()(override val ws: WSClient,
                                         //                                         override val metrics: Metrics,
                                         cfg: ContentClientConfig,
                                         sectionMetadataService: LegacySectionService)
  extends AbstractService[Seq[ApiContent]] with ContentSearchService {

  import ApiReads._

  override def config: ServiceConfiguration = cfg.getServiceConfig(serviceName)

  override def jsonValidate: (JsLookupResult) => JsResult[Seq[ApiContent]] = json => json.validate[Seq[ApiContent]]

  override def search(maybePath: Option[String], maybeTyp: Option[String], maybeSubType: Option[String], flags: Set[String], limit: Option[Int])
                     (implicit executionContext: ExecutionContext): Future[Seq[EnrichedApiContent]] = {

    /**
      * filter invalid values (such as negative or too large)
      *
      * @param maybeLimit the requested limit
      * @return sanitized limit:
      *         the value, if within valid bounds
      *         max allowed is `ContentSearchServiceImpl.maxResultSize`
      *         `ContentSearchServiceImpl.defaultResultSize` if `None` was passed
      */
    def sanitizeLimit(maybeLimit: Option[Int]) = {
      import ContentSearchServiceImpl._
      maybeLimit.map(l => Math.abs(Math.min(l, maxResultSize))) getOrElse defaultResultSize
    }

    val parameters = Seq("resultSize" -> sanitizeLimit(limit).toString) ++
      maybePath.map("sectionPath" -> _) ++
      maybeTyp.map("type" -> _) ++
      maybeSubType.map("subType" -> _) ++
      flags.map("flags" -> _)
//        subTypeExcludes.map ( "excludes" -> _ )

    get(Nil, parameters)
      .map { responses =>
        responses.map { content =>
          sectionMetadataService.enrich(content)
        }
      }
  }
}

object ContentSearchServiceImpl {
  val defaultResultSize = 12
  val maxResultSize = 24
}
