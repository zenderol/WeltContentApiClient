package de.welt.contentapi.raw.admin.client.services

import java.time.Instant
import javax.inject.{Inject, Singleton}

import com.google.common.base.Stopwatch
import com.google.inject.ImplementedBy
import de.welt.contentapi.core.client.services.CapiExecutionContext
import de.welt.contentapi.core.client.services.s3.S3Client
import de.welt.contentapi.raw.client.services.{RawTreeService, RawTreeServiceImpl}
import de.welt.contentapi.raw.models.{ChannelUpdate, FullRawChannelWrites, RawChannel}
import de.welt.contentapi.utils.Env.{Env, Live, Preview}
import play.api.libs.json.{JsValue, Json}
import play.api.{Configuration, Environment, Logger}

@ImplementedBy(classOf[AdminSectionServiceImpl])
trait AdminSectionService extends RawTreeService {

  def updateChannel(channel: RawChannel,
                    channelWithUpdates: RawChannel,
                    user: String)(implicit env: Env): Option[RawChannel]

  /**
    * Syncs the current rawTree with the tree from Static Dump Provider to get new or changed Channels
    */
  def syncWithLegacy(): Unit

}

@Singleton
class AdminSectionServiceImpl @Inject()(config: Configuration,
                                        s3: S3Client,
                                        environment: Environment,
                                        legacySectionService: SdpSectionDataService,
                                        capiContext: CapiExecutionContext)
  extends RawTreeServiceImpl(s3, config, environment, capiContext) with AdminSectionService {

  override def updateChannel(channel: RawChannel, channelWithUpdates: RawChannel, user: String)
                            (implicit env: Env): Option[RawChannel] = {

    // update channel
    channel.config = channelWithUpdates.config
    // modify meta data
    channel.metadata = channel.metadata.copy(
      lastModifiedDate = Instant.now.toEpochMilli,
      changedBy = user
    )
    // update the stages/modules
    channel.stageConfiguration = channelWithUpdates.stageConfiguration

    Logger.info(s"$channel changed by $user")

    // save changes
    save

    // reload changes from s3
    val freshRootNode = root(env)
    val updatedChannel = freshRootNode.flatMap(_.findByPath(channel.id.path))
    Logger.debug(s"Updated Channel from fresh s3 data $updatedChannel")
    updatedChannel
  }

  override def syncWithLegacy(): Unit = {
    def mergeAndSave(updates: RawChannel, env: Env): Option[ChannelUpdate] = {
      val maybeRoot = root(env)
      val mergeResult = maybeRoot.map(root => ChannelTools.merge(root, updates))
      maybeRoot.foreach(root ⇒ saveChannel(root)(env))
      mergeResult
    }

    Logger.info(s"[Sync] starting sync. ")
    val stopwatch = Stopwatch.createStarted()
    val updates = legacySectionService.getSectionData.toChannel

    mergeAndSave(updates, Preview)
    val mergeUpdate = mergeAndSave(updates, Live)

    mergeUpdate.foreach(update ⇒ Logger.info(s"[Sync] Done syncing with legacy, found the following changes: $update"))
    Logger.info(s"[Sync] took ${stopwatch.stop.toString}")
  }

  override def root(env: Env): Option[RawChannel] = super.root(env)
    .orElse(getTreeForEnv(env).map(_.channel))
    .orElse {
    Logger.warn("No data found in s3 bucket, creating new data set from scratch.")
    val root = legacySectionService.getSectionData.toChannel

    saveChannel(root)(Preview)
    saveChannel(root)(Live)
    Some(root)
  }

  private def saveChannel(ch: RawChannel)(implicit env: Env) = {

    val json: JsValue = Json.toJson(ch)(FullRawChannelWrites.channelWrites)
    val serializedChannelData = json.toString

    Logger.info(s"saving channel tree to s3 -> ${objectKeyForEnv(env)}")

    s3.putPrivate(bucket, objectKeyForEnv(env), serializedChannelData, "application/json")
  }

  def save(implicit env: Env) = {
    root(env).foreach(r ⇒ saveChannel(r))
    Logger.debug("Invalidating cache.")
    update()
  }
}

object ChannelTools {

  def diff(current: RawChannel, update: RawChannel): ChannelUpdate = {

    if (current != update) {
      Logger.debug(s"Cannot diff($current, $update, because they are not .equal()")
      ChannelUpdate(Seq.empty, Seq.empty, Seq.empty)
    } else {

      val bothPresentIds = current.children.map(_.id).intersect(update.children.map(_.id))
      val updatesFromChildren = bothPresentIds.flatMap { id ⇒
        val tupleOfMatchingChannels = current.children.find(_.id == id).zip(update.children.find(_.id == id))

        tupleOfMatchingChannels.map { tuple ⇒
          diff(tuple._1, tuple._2)
        }
      }
      // elements that are no longer in `other.children`
      val deletedByOther = current.children.diff(update.children)
      // additional elements from `other.children`
      val addedByOther = update.children.diff(current.children)

      val moved = {

        // if we can find it in our tree, it hasn't been added but only moved
        val notAddedButMoved = addedByOther.flatMap { e ⇒ current.root.findByEscenicId(e.id.escenicId) }

        lazy val otherRoot = update.root
        // if we can find the deleted elem, it has been moved
        val notDeletedButMoved = deletedByOther.filter { elem ⇒ otherRoot.findByEscenicId(elem.id.escenicId).isDefined }

        notAddedButMoved ++ notDeletedButMoved
      }

      val deleted = deletedByOther.diff(moved)
      val added = addedByOther.diff(moved)

      val channelUpdate = ChannelUpdate(added, deleted, moved).merge(updatesFromChildren)
      if (!channelUpdate.isEmpty) {
        Logger.debug(s"[$this] Changes: $channelUpdate\n\n")
      }
      channelUpdate
    }
  }

  def merge(current: RawChannel, other: RawChannel): ChannelUpdate = {

    val channelUpdate = diff(current, other)

    channelUpdate.deleted.foreach { deletion ⇒
      deletion.parent.foreach { parent ⇒
        parent.children = parent.children.filterNot(_ == deletion)
      }
    }

    channelUpdate.added.foreach { addition ⇒
      current.children = current.children :+ addition
    }

    channelUpdate.moved.foreach { moved ⇒
      // remove from current parent
      moved.parent.foreach { parent ⇒
        parent.children = parent.children.filterNot(_ == moved)
      }
      // add to new parent
      val newParentId = other.findByEscenicId(moved.id.escenicId)
        .flatMap(_.parent)
        .map(_.id.escenicId)

      newParentId.foreach { parentId ⇒
        current.root.findByEscenicId(parentId).foreach { newParent ⇒
          newParent.children = newParent.children :+ moved
        }
      }
    }

    // update master data (path and displayName) from legacy source
    updateData(current, other)

    // for logging
    channelUpdate
  }

  /**
    * copies some data from the `legacyRoot` tree to the `current` tree
    *
    * @param current    the destination where to write updates to
    * @param legacyRoot the source object where to read updates from
    */
  def updateData(current: RawChannel, legacyRoot: RawChannel): Unit = {
    legacyRoot.findByEscenicId(current.id.escenicId).foreach(other ⇒ current.updateMasterData(other))
    current.children.foreach(child ⇒ updateData(child, legacyRoot))
  }


}




