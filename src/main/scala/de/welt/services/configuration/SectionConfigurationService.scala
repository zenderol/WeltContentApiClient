package de.welt.services.configuration

import javax.inject.{Inject, Singleton}

import de.welt.models.config
import de.welt.models.config.{Channel, ChannelData, ChannelId, Env}
import de.welt.services.contentapi.LegacySectionService
import de.welt.services.s3.S3
import de.welt.traits.Loggable
import play.api.cache.CacheApi
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.{Environment, Mode}

import scala.concurrent.duration._

trait SectionConfigurationService {

  def channelByPath(id: ChannelId)(implicit env: Env): Option[Channel]

}

@Singleton
class SectionConfigurationServiceImpl @Inject()(funkConfig: ContentClientConfig,
                                                s3: S3,
                                                environment: Environment,
                                                legacySectionService: LegacySectionService,
                                                cache: CacheApi)
  extends SectionConfigurationService with Loggable {

  override def channelByPath(id: ChannelId)(implicit env: Env): Option[Channel] = {
    root.findByPath(id.path)
  }

  private def objectKeyForEnv(env: Env) = environment.mode match {
    case Mode.Prod ⇒ s"janus2/prod/${env.toString}/config.json"
    case _ ⇒ s"janus2/dev/${env.toString}/config.json"
  }

  private[configuration] def root(implicit env: Env): Channel = cache.getOrElse(env.toString, 10.minutes) {
    import config.WithChildrenReads._

    val maybeData: Option[String] = s3.get(funkConfig.aws.s3.janus.bucket, objectKeyForEnv(env))

    maybeData
      .map { data ⇒ Json.parse(data).validate[Channel] }
      .flatMap {
        case JsSuccess(v, _) ⇒
          Some(v)
        case err@JsError(_) ⇒
          log.warn(err.toString)
          None
      }.getOrElse {
      log.warn("No data found in s3 bucket.")
      throw new IllegalStateException("no channel data")
    }

  }
}




