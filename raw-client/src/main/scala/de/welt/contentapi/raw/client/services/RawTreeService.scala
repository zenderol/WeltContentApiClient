package de.welt.contentapi.raw.client.services

import javax.inject.{Inject, Singleton}

import de.welt.contentapi.core.client.services.s3.S3Client
import de.welt.contentapi.raw.models.RawChannel
import de.welt.contentapi.utils.Env.Env
import de.welt.contentapi.utils.Loggable
import play.api.{Configuration, Environment, Mode}
import play.api.cache.CacheApi
import play.api.libs.json.{JsError, JsSuccess, Json}

import scala.concurrent.duration._

trait RawTreeService {
  def root(implicit env: Env): Option[RawChannel]
}

@Singleton
class RawTreeServiceImpl @Inject()(s3Client: S3Client,
                                   config: Configuration,
                                   environment: Environment,
                                   cache: CacheApi) extends RawTreeService with Loggable {

  import RawTreeServiceImpl._

  /**
    * S3 bucket name
    */
  val bucket: String = config.getString(bucketConfigKey)
    .getOrElse(throw config.reportError(bucketConfigKey, bucketConfigKey + " bucket not configured"))

  /**
    * S3 root folder for the raw tree
    */
  val folder: String = config.getString(folderConfigKey).orElse {
    log.warn(s"Config Key [$fileConfigKey] is deprecated. Please use: [$folderConfigKey]")
    config.getString(fileConfigKey)
  } getOrElse { throw config.reportError(folderConfigKey, folderConfigKey + " file not configured") }

  /**
    * prod/dev/local-dev mode
    * This is only a sub folder with the Live/Preview raw tree
    */
  private val maybeMode: Option[String] = config.getString(modeConfigKey)

  // todo (all): let's talk about the folder structure
  protected def objectKeyForEnv(env: Env): String = {
    // playMode is a fallback for api-client-version >0.6.x
    val playMode: String = environment.mode match {
      case Mode.Prod ⇒ "prod"
      case _ ⇒ "dev"
    }
    val mode: String = maybeMode.getOrElse(playMode)
    s"$folder/$mode/${env.toString}/config.json"
  }

  // todo (mana): add metrics
  def root(implicit env: Env): Option[RawChannel] = {
    cache.getOrElse(s"rawChannelData-$env", 1.minutes) {
      s3Client.get(bucket, objectKeyForEnv(env)).flatMap { tree ⇒
        Json.parse(tree).validate[RawChannel](de.welt.contentapi.raw.models.RawReads.rawChannelReads) match {
          case JsSuccess(root, _) ⇒
            log.info(s"Loaded/Refreshed raw tree for $env")
            root.updateParentRelations()
            Some(root)
          case e: JsError ⇒
            log.error(f"JsError parsing S3 file: '$bucket/$folder'. " + JsError.toJson(e).toString())
            None
        }
      }
    }
  }
}

object RawTreeServiceImpl {
  val bucketConfigKey = "welt.aws.s3.rawTree.bucket"
  val folderConfigKey = "welt.aws.s3.rawTree.folder"
  val modeConfigKey = "welt.aws.s3.rawTree.mode"

  // @deprecated since 01/2017. Use folderConfigKey
  // TODO (PaDa) : (PaDa) Remove when all clients using `folderConfigKey`
  @Deprecated
  val fileConfigKey = "welt.aws.s3.rawTree.file"
}
