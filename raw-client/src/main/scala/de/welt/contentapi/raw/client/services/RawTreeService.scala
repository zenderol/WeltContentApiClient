package de.welt.contentapi.raw.client.services

import java.time.Instant
import javax.inject.{Inject, Singleton}

import com.google.inject.ImplementedBy
import de.welt.contentapi.core.client.services.CapiExecutionContext
import de.welt.contentapi.core.client.services.s3.S3Client
import de.welt.contentapi.raw.models.RawChannel
import de.welt.contentapi.utils.Env.{Env, Live}
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.{Configuration, Environment, Logger, Mode}

import scala.collection.mutable
import scala.concurrent.duration._

@ImplementedBy(classOf[RawTreeServiceImpl])
trait RawTreeService {
  def root(env: Env): Option[RawChannel]
}

@Singleton
class RawTreeServiceImpl @Inject()(s3Client: S3Client,
                                   config: Configuration,
                                   environment: Environment,
                                   implicit val capiContext: CapiExecutionContext) extends RawTreeService {

  import RawTreeServiceImpl._

  private val data: mutable.Map[Env, RawTreeRapper] = mutable.Map()
  protected[services] val bucket: String = config.get[String](bucketConfigKey)
  protected[services] val folder: String = config.get[String](folderConfigKey)

  // start cron to update the tree automatically
  capiContext.actorSystem.scheduler.schedule(1.minute, 1.minute, () ⇒ update())

  // always cron-based update Env: Live
  override def root(env: Env): Option[RawChannel] = data.get(env).map(_.channel)

  /**
    * prod/dev/local-dev mode
    * This is only a sub folder with the Live/Preview raw tree
    */
  private val mode: String = config.getOptional[String](modeConfigKey).getOrElse {
    // playMode is a fallback for api-client-version >0.6.x
    environment.mode match {
      case Mode.Prod ⇒ "prod"
      case _ ⇒ "dev"
    }
  }
  // initially update the tree
  update()

  protected def objectKeyForEnv(env: Env): String = s"$folder/$mode/${env.toString}/config.json"

  def update(): Unit = {
    val maybeRemote = s3Client.getLastModified(bucket, objectKeyForEnv(Live))
    val maybeLocal = data.get(Live).map(_.lastMod).orElse(Some(Instant.now()))
    for {
      remoteLastMod ← maybeRemote
      localLastMod ← maybeLocal
      if remoteLastMod != localLastMod
      remoteState ← getTreeForEnv(Live)
    } yield {
      Logger.info(s"Remote raw tree for env $Live was changed. Replacing local ($localLastMod) with remote state ($remoteLastMod).")
      data += Live → RawTreeRapper(remoteState.channel, remoteState.lastMod)
    }
  }

  protected def getTreeForEnv(env: Env): Option[RawTreeRapper] = {
    s3Client.getWithLastModified(bucket, objectKeyForEnv(env)).flatMap { remoteState ⇒
      Json.parse(remoteState._1).validate[RawChannel](de.welt.contentapi.raw.models.RawReads.rawChannelReads) match {
        case JsSuccess(parsedTree, _) ⇒
          Logger.info(s"Downloaded and parsed raw tree for $env")
          parsedTree.updateParentRelations()
          Some(RawTreeRapper(parsedTree, remoteState._2))
        case e: JsError ⇒
          Logger.error(f"JsError parsing S3 file: '$bucket/$folder'. " + JsError.toJson(e).toString())
          None
      }
    }
  }
}

object RawTreeServiceImpl {
  val bucketConfigKey = "welt.aws.s3.rawTree.bucket"
  val folderConfigKey = "welt.aws.s3.rawTree.folder"
  val modeConfigKey = "welt.aws.s3.rawTree.mode"
}

case class RawTreeRapper(channel: RawChannel, lastMod: Instant)
