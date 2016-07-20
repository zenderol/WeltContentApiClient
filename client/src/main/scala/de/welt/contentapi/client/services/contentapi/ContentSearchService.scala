package de.welt.contentapi.client.services.contentapi

import javax.inject.{Inject, Singleton}

import com.kenshoo.play.metrics.Metrics
import de.welt.contentapi.client.services.configuration.{ContentClientConfig, ServiceConfiguration}
import de.welt.contentapi.core.models.{ApiContent, EnrichedApiContent}
import play.api.libs.json.{JsLookupResult, JsResult}
import play.api.libs.ws.WSClient
import play.api.mvc.Request

import scala.concurrent.{ExecutionContext, Future}

sealed trait ContentSearchService {
  protected val serviceName = "search"

  /**
    * @param path  section path. example: /politik/deutschland/
    * @param typ   content type to search for. example: article, gallery, video
    * @param flags filters, such as HIGHLIGHT or PREMIUM
    * @param limit limit the resultSize
    */
  def search(path: Option[String],
             typ: Option[String] = None,
             subType: Option[String] = None,
             subTypeExcludes: Iterable[String] = Iterable.empty,
             flags: Iterable[String] = Iterable.empty,
             limit: Option[Int] = None)
            (implicit request: Request[Any], executionContext: ExecutionContext): Future[Seq[EnrichedApiContent]]
}

@Singleton
class ContentSearchServiceImpl @Inject()(override val ws: WSClient,
                                         override val metrics: Metrics,
                                         cfg: ContentClientConfig,
                                         sectionService: SectionService)
  extends AbstractService[Seq[ApiContent]] with ContentSearchService {

  import de.welt.contentapi.core.models.ApiReads._

  override def config: ServiceConfiguration = cfg.getServiceConfig(serviceName)

  override def jsonValidate: (JsLookupResult) => JsResult[Seq[ApiContent]] = json => json.validate[Seq[ApiContent]]

  override def search(path: Option[String],
                      typ: Option[String],
                      subType: Option[String],
                      subTypeExcludes: Iterable[String],
                      flags: Iterable[String],
                      limit: Option[Int])
                     (implicit request: Request[Any], executionContext: ExecutionContext): Future[Seq[EnrichedApiContent]] = {

    val parameters = Seq("resultSize" -> ContentSearchService.sanitizeLimit(limit).toString) ++
      path.map("sectionPath" → _) ++
      typ.map("type" → _) ++
      subType.map("subType" → _) ++
      flags.map("flags" → _) ++
      subTypeExcludes.map("excludes" → _)

    get(Nil, parameters, Nil).map { responses ⇒
      responses.map { content ⇒
        sectionService.enrich(content)
      }
    }
  }

}

object ContentSearchService {
  val defaultResultSize = 12
  val maxResultSize = 24

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
    maybeLimit.map(l => Math.abs(Math.min(l, maxResultSize))) getOrElse defaultResultSize
  }
}
