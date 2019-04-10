package de.welt.contentapi.raw.client.services

import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

import com.google.inject.ImplementedBy
import de.welt.contentapi.core.client.services.CapiExecutionContext
import de.welt.contentapi.core.client.services.aws.s3.S3Client
import de.welt.contentapi.core.client.services.configuration.{ApiConfiguration, Environment}
import de.welt.contentapi.raw.models.RawChannel
import de.welt.contentapi.utils.Loggable
import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsError, JsSuccess, Json}

import scala.concurrent.duration._

@ImplementedBy(classOf[RawTreeServiceImpl])
trait RawTreeService {
  def root: Option[RawChannel]
}

@Singleton
class RawTreeServiceImpl @Inject()(s3Client: S3Client,
                                   implicit val capiContext: CapiExecutionContext)
  extends RawTreeService with Loggable {

  private val data: AtomicReference[RawTreeRapper] = new AtomicReference[RawTreeRapper]()
  protected[services] val bucket: String = ApiConfiguration.aws.s3.raw.bucket
  protected[services] val file: String = ApiConfiguration.aws.s3.raw.file

  // always cron-based update Env: Live
  override def root: Option[RawChannel] = Option(data.get()).map(_.channel)

  // initially update the tree
  if (Environment.stage.isTest) {
    log.info("RawTree will not be loaded when started in Mode.Test. If you require section data, please mock it.")
  } else {
    // start cron to update the tree automatically
    capiContext.actorSystem.scheduler.schedule(1.minute, 1.minute, () ⇒ update())
    update()
  }

  def update(): Unit = {
    val maybeRemote = s3Client.getLastModified(bucket, file)
    val maybeLocal = Option(data.get()).map(_.lastMod).orElse(Some(Instant.now()))
    for {
      remoteLastMod ← maybeRemote
      localLastMod ← maybeLocal
      if remoteLastMod != localLastMod
      remoteState ← getTree
    } yield {
      log.debug(s"Remote raw tree was changed. Replacing local ($localLastMod) with remote state ($remoteLastMod).")
      data.set(RawTreeRapper(remoteState.channel, remoteState.lastMod))
    }
  }

  protected def getTree: Option[RawTreeRapper] = {
    s3Client.getWithLastModified(bucket, file).flatMap { remoteState ⇒
      Json.parse(remoteState._1).validate[RawChannel](de.welt.contentapi.raw.models.RawReads.rawChannelReads) match {
        case JsSuccess(parsedTree, _) ⇒
          log.debug(s"Downloaded and parsed raw tree from $bucket/$file")
          parsedTree.updateParentRelations()
          Some(RawTreeRapper(parsedTree, remoteState._2))
        case e: JsError ⇒
          log.error(f"JsError parsing S3 file: '$bucket/$file'. " + JsError.toJson(e).toString())
          None
      }
    }
  }
}

case class RawTreeRapper(channel: RawChannel, lastMod: Instant)
