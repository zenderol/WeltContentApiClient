package de.welt.contentapi.legacy.client

import javax.inject.{Inject, Singleton}

import com.kenshoo.play.metrics.Metrics
import de.welt.contentapi.core.client.services.configuration.ServiceConfiguration
import de.welt.contentapi.core.client.services.contentapi.AbstractService
import de.welt.contentapi.core.client.services.http.RequestHeaders
import de.welt.contentapi.legacy.models.{ApiLegacyPressedSection, ApiLegacyPressedStage, ApiLegacySection, ApiLegacyStage}
import de.welt.contentapi.pressed.client.converter.RawToApiConverter
import de.welt.contentapi.pressed.client.services.PressedContentService
import de.welt.contentapi.raw.client.services.RawTreeService
import de.welt.contentapi.raw.models.RawChannel
import de.welt.contentapi.utils.Env
import play.api.Configuration
import play.api.libs.json.{JsLookupResult, JsResult}
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}

trait LegacySectionService {
  def getByPath(path: String)(implicit requestHeaders: RequestHeaders = Seq.empty,
                              executionContext: ExecutionContext): Future[ApiLegacyPressedSection]
}

@Singleton
class LegacySectionServiceImpl @Inject()(pressedContentService: PressedContentService,
                                         treeService: RawTreeService,
                                         converter: RawToApiConverter,
                                         override val configuration: Configuration,
                                         override val ws: WSClient,
                                         override val metrics: Metrics)
  extends AbstractService[ApiLegacySection] with LegacySectionService {

  import de.welt.contentapi.legacy.models.LegacyFormats._

  override val serviceName: String = "legacy.section"
  override val jsonValidate: (JsLookupResult) ⇒ JsResult[ApiLegacySection] = json => json.validate[ApiLegacySection]

  override def getByPath(path: String)
                        (implicit requestHeaders: RequestHeaders  = Seq.empty,
                         executionContext: ExecutionContext): Future[ApiLegacyPressedSection] = {

    val maybeRawChannel: Option[RawChannel] = findChannelForSection(path)

    super.get(urlArguments = Seq(path))
      .map { apiSection: ApiLegacySection ⇒
        ApiLegacyPressedSection(
          stages = apiSection.lists.map(stages ⇒ stages.map(convert)),
          channel = maybeRawChannel.map(rawChannel ⇒ converter.apiChannelFromRawChannel(rawChannel)),
          configuration = maybeRawChannel.map(rawChannel ⇒ converter.apiConfigurationFromRawChannel(rawChannel))
        )
      }
  }


  private def convert(apiLegacySection: ApiLegacyStage): ApiLegacyPressedStage = ApiLegacyPressedStage(
    id = apiLegacySection.id,
    label = apiLegacySection.label,
    content = Option(apiLegacySection.unwrappedContent.map(c ⇒ pressedContentService.convert(c, related = None)))
  )

  private def findChannelForSection(sectionPath: String): Option[RawChannel] = {
    val cleanedSectionPath: String = sectionPath match {
      case "home" ⇒ "/"
      case valid@_ ⇒ valid
    }
    treeService.root(env = Env.Live).flatMap { rawTree ⇒ rawTree.findByPath(cleanedSectionPath) }
  }

}
