package de.welt.contentapi.pressed.client.services

import com.codahale.metrics.Timer.Context
import com.google.inject.ImplementedBy
import com.kenshoo.play.metrics.Metrics
import de.welt.contentapi.core.client.services.CapiExecutionContext
import de.welt.contentapi.core.client.services.http.RequestHeaders
import de.welt.contentapi.core.models.ApiContent
import de.welt.contentapi.pressed.client.converter.RawToApiConverter
import de.welt.contentapi.pressed.models.{ApiChannel, ApiConfiguration, ApiPressedContent}
import de.welt.contentapi.raw.client.services.RawTreeService
import de.welt.contentapi.raw.models.RawChannel
import javax.inject.{Inject, Singleton}

import scala.concurrent.Future

@ImplementedBy(classOf[PressedContentServiceImpl])
trait PressedContentService {
  /**
    * Uses the ContentService to get an ApiContent from ContentAPI and wraps it with the convert method as ApiPressedContent
    *
    * @param id          Escenic ID of the content
    * @param showRelated flag for requesting related articles
    * @return a Future[ApiPressedContent]
    */
  def find(id: String, showRelated: Boolean = true)
          (implicit requestHeaders: RequestHeaders = Nil): Future[ApiPressedContent]

  /**
    * Wraps ApiContent as ApiPressedContent with its Channel as ApiChannel and configuration data
    *
    * @param apiContent the content to wrap
    * @param related    its related articles
    * @return the wrapped ApiContent as ApiPressedContent
    */
  def convert(apiContent: ApiContent, related: Option[Seq[ApiContent]] = None): ApiPressedContent

  /**
    * Wraps ApiContent as ApiPressedContent just with its Channel but no Configuration or related Content
    *
    * @param apiContent the content to wrap
    * @return ApiPressedContent containing the ApiContent and its ApiChannel
    */
  def pressSingleApiContent(apiContent: ApiContent): ApiPressedContent
}

@Singleton
class PressedContentServiceImpl @Inject()(ciggerService: CiggerService,
                                          converter: RawToApiConverter,
                                          rawTreeService: RawTreeService,
                                          metrics: Metrics,
                                          implicit val capi: CapiExecutionContext)
  extends PressedContentService {

  override def find(id: String, showRelated: Boolean = true)
                   (implicit requestHeaders: RequestHeaders = Nil): Future[ApiPressedContent] = {

    ciggerService.byId(id, showRelated).map(_.result)
  }

  override def convert(apiContent: ApiContent, maybeRelatedContent: Option[Seq[ApiContent]] = None): ApiPressedContent = {
    val timer: Context = metrics.defaultRegistry.timer("PressedContentService.convert").time()
    val maybeRelatedPressedContent: Option[Seq[ApiPressedContent]] = maybeRelatedContent
      .map(related ⇒ related.map(pressSingleApiContent))

    val content: ApiPressedContent = findRawChannel(apiContent).map { rawChannel ⇒
      val apiChannel: ApiChannel = converter.apiChannelFromRawChannel(rawChannel)
      val apiConfiguration: ApiConfiguration = converter.apiConfigurationFromRawChannel(rawChannel)
      ApiPressedContent(
        content = apiContent,
        related = maybeRelatedPressedContent,
        channel = Some(apiChannel),
        configuration = Some(apiConfiguration),
        embeds = None
      )
    } getOrElse {
      // Fallback if S3.get or Json.parse fails
      ApiPressedContent(
        content = apiContent,
        related = maybeRelatedPressedContent
      )
    }
    timer.stop
    content
  }

  override def pressSingleApiContent(apiContent: ApiContent): ApiPressedContent =
    ApiPressedContent(
      content = apiContent,
      channel = findRawChannel(apiContent).map(converter.apiChannelFromRawChannel)
    )

  private def findRawChannel(apiContent: ApiContent): Option[RawChannel] = rawTreeService.root
    .flatMap { rawTree ⇒
      apiContent
        .sections
        .flatMap(_.home)
        .flatMap(rawTree.findByPath)
    }
}
