package de.welt.contentapi.client.services.contentapi

import javax.inject.{Inject, Singleton}

import de.welt.contentapi.client.services.configuration.ContentClientConfig
import de.welt.contentapi.client.services.s3.S3
import de.welt.contentapi.core.models.reads.FullChannelReads
import de.welt.contentapi.core.models.{EnrichedApiContent, _}
import de.welt.contentapi.core.traits.Loggable
import play.api.cache.CacheApi
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.{Environment, Mode}

import scala.concurrent.duration._

trait SectionService {

  def findChannel(path: String)(implicit env: Env): Option[Channel]

  def enrich(apiContent: ApiContent): EnrichedApiContent

}

@Singleton
class SectionServiceImpl @Inject()(config: ContentClientConfig,
                                   cache: CacheApi,
                                   s3: S3,
                                   environment: Environment)
  extends SectionService with Loggable {

  override def findChannel(path: String)(implicit env: Env): Option[Channel] = root.flatMap(_.findByPath(path))

  override def enrich(apiContent: ApiContent): EnrichedApiContent = {

    val maybeSectionData: Option[SectionData] = apiContent.sections.flatMap {
      _.home.flatMap { home ⇒ {
        val maybeHomeSection: Option[Channel] = root(Live).flatMap(_.findByPath(home))
        maybeHomeSection.map { homeSection ⇒
          SectionData(homeSection, breadcrumb(homeSection))
        }
      }
      }
    }
    EnrichedApiContent(apiContent, maybeSectionData)
  }

  protected def breadcrumb(home: Channel): Seq[Channel] = {
    val breadcrumbPath = home.id.path.split('/').filter(_.nonEmpty).toList match {
      case Nil ⇒ Nil
      case head :: tail ⇒ tail.scanLeft(s"/$head/")((path, s) ⇒ path + s + "/")
    }

    val breadcrumbChannels: List[Channel] = breadcrumbPath.flatMap(segment ⇒ root(Live).flatMap(_.findByPath(segment)))
    SectionConfigurationServiceImpl.fakeRoot :: breadcrumbChannels
  }


  protected def root(implicit env: Env): Option[Channel] = cache.getOrElse(env.toString, 10.minutes) {

    import FullChannelReads._

    s3.get(config.aws.s3.janus.bucket, objectKeyForEnv(env))
      .map { data ⇒ Json.parse(data).validate[Channel] }
      .flatMap {
        case JsSuccess(v, _) ⇒
          v.updateParentRelations()
          Some(v)
        case err@JsError(_) ⇒
          log.warn(err.toString)
          None
      }

  }

  protected def objectKeyForEnv(env: Env) = environment.mode match {
    case Mode.Prod ⇒ s"${config.aws.s3.janus.file}/prod/${env.toString}/config.json"
    case _ ⇒ s"${config.aws.s3.janus.file}/dev/${env.toString}/config.json"
  }
}

object SectionConfigurationServiceImpl {
  // One Slash for root node is needed for traversing the channels (e.g. in def breadcrumb()).
  val fakeRoot = Channel(ChannelId("/"), ChannelData("Startseite"))
}
