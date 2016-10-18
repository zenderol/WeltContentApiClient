package de.welt.contentapi.core.models

import java.time.Instant

import de.welt.contentapi.core.traits.Loggable

import scala.annotation.tailrec
import scala.collection.mutable

case class ApiChannel(id: ChannelId,
                      var data: ApiChannelData,
                      var stages: Option[Seq[Stage]] = None,
                      var parent: Option[ApiChannel] = None,
                      var children: Seq[ApiChannel] = Seq.empty,
                      var hasChildren: Boolean = false,
                      var lastModifiedDate: Long = Instant.now.toEpochMilli,
                      var metadata: Option[ApiChannelMetadataNew] = None) extends Loggable  {

  hasChildren = children.nonEmpty
  val DEFAULT_AD_TAG = "sonstiges"

  /**
    * The default root channel label is "WON frontpage". Funkotron needs "Home".
    */
  private val renameRootChannel: (ApiChannel) => ApiChannel = (root: ApiChannel) => {
    root.copy(data = root.data.copy(label = "Home"))
  }
  
  final def updateParentRelations(newParent: Option[ApiChannel] = None): Unit = {
    this.parent = newParent
    children.foreach(_.updateParentRelations(Some(this)))
  }

  def getBreadcrumb(): Seq[ApiChannel] = {
    var breadcrumb: mutable.MutableList[ApiChannel] = mutable.MutableList(this.copy(children = Nil))
    var current: Option[ApiChannel] = getParentSafeley(this)
    while (current.isDefined) {
      breadcrumb.++=(Seq(current.get.copy(children = Nil)))
      current = getParentSafeley(current.get)
    }
    breadcrumb.reverse.toList match {
      case head :: tail => Seq(renameRootChannel(head)) ++ tail
      case _ => Nil
    }
  }

  def getParentSafeley(channel: ApiChannel) : Option[ApiChannel] = {
    if (channel.parent.isDefined && channel.parent.get.id.ece != 0) {
      Some(channel.parent.get)
    } else {
      None
    }
  }

  def getAdTag: Option[String] = {
    if (data.adData.definesAdTag) {
      val adPath: String = id.path.stripPrefix("/").stripSuffix("/").trim match {
        case empty if empty.isEmpty && id.ece == 5 => "home"
        case ok if !ok.isEmpty => ok
        case _ => DEFAULT_AD_TAG
      }
      Some(adPath)
    } else {
      parent.flatMap(_.getAdTag)
    }
  }

  final def findByEce(ece: Long): Option[ApiChannel] = {
    if (id.ece == ece) {
      Some(this)
    } else {
      children.flatMap { ch ⇒ ch.findByEce(ece) }.headOption
    }
  }

  def findByPath(search: String): Option[ApiChannel] = findByPath(
    search.split('/').filter(_.nonEmpty).toList match {
      case Nil ⇒ Nil
      case head :: tail ⇒ tail.scanLeft(s"/$head/")((path, s) ⇒ path + s + "/")
    }
  )

  private def findByPath(sectionPath: Seq[String]): Option[ApiChannel] = {
    sectionPath match {
      case Nil ⇒
        Some(this)
      case head :: Nil ⇒
        children.find(_.id.path == head)
      case head :: tail ⇒
        children.find(_.id.path == head).flatMap(_.findByPath(tail))
    }
  }

  override def toString: String = s"Channel(id='${id.path}', ece=${id.ece}'')"

  @tailrec
  final def root: ApiChannel = parent match {
    case Some(p) ⇒ p.root
    case None ⇒ this
  }

  def diff(other: ApiChannel): ChannelUpdate = {

    if (this != other) {
      log.debug(s"Cannot diff($this, $other, because they are not .equal()")
      ChannelUpdate(Seq.empty, Seq.empty, Seq.empty)
    } else {

      val bothPresentIds = this.children.map(_.id).intersect(other.children.map(_.id))
      val updatesFromChildren = bothPresentIds.flatMap { id ⇒
        val tupleOfMatchingChannels = this.children.find(_.id == id).zip(other.children.find(_.id == id))

        tupleOfMatchingChannels.map { tuple ⇒
          tuple._1.diff(tuple._2)
        }
      }
      // elements that are no longer in `other.children`
      val deletedByOther = this.children.diff(other.children)
      // additional elements from `other.children`
      val addedByOther = other.children.diff(this.children)

      log.debug(s"[$this] added locally: $addedByOther")
      log.debug(s"[$this] deleted locally: $deletedByOther")

      val moved = {
        lazy val thisRoot = this.root

        // if we can find it in our tree, it hasn't been added but only moved
        val notAddedButMoved = addedByOther.filter { elem ⇒ thisRoot.findByEce(elem.id.ece).isDefined }
        log.debug(s"[$this] not added but moved: $notAddedButMoved")

        lazy val otherRoot = other.root
        // if we can find the deleted elem, it has been moved
        val notDeletedButMoved = deletedByOther.filter { elem ⇒ otherRoot.findByEce(elem.id.ece).isDefined }
        log.debug(s"[$this] not deleted but moved: $notDeletedButMoved")

        notAddedButMoved ++ notDeletedButMoved
      }
      log.debug(s"[$this] moved: $moved")

      val deleted = deletedByOther.diff(moved)
      val added = addedByOther.diff(moved)

      log.debug(s"[$this] deleted globally: $deleted")
      log.debug(s"[$this] added globally: $added")

      val u = ChannelUpdate(added, deleted, moved).merge(updatesFromChildren)
      log.debug(s"[$this] Changes: $u\n\n")
      u
    }
  }

  def merge(other: ApiChannel): ChannelUpdate = {

    val channelUpdate = diff(other)

    channelUpdate.deleted.foreach { deletion ⇒
      deletion.parent.foreach { parent ⇒
        parent.children = parent.children.diff(Seq(deletion))
      }
    }

    channelUpdate.added.foreach { addition ⇒
      this.children = this.children :+ addition
    }

    channelUpdate.moved.foreach { moved ⇒
      // remove from current parent
      moved.parent.foreach { parent ⇒
        parent.children = parent.children.diff(Seq(moved))
      }
      // add to new parent
      val newParentId = other.findByEce(moved.id.ece)
        .flatMap(_.parent)
        .map(_.id.ece)

      newParentId.foreach { parentId ⇒
        root.findByEce(parentId).foreach { newParent ⇒
          newParent.children = newParent.children :+ moved
        }
      }
    }
    // for logging
    channelUpdate
  }

  /**
    * apply updates to the [[ApiChannelData]] and [[ChannelId]] from another [[ApiChannel]]
    *
    * @param other the source for the changes
    */
  def updateMasterData(other: ApiChannel) = {
    id.path = other.id.path
    data = data.copy(label = other.data.label)
    lastModifiedDate = Instant.now.toEpochMilli
  }

  // todo remove after model updates are persisted
  def applyChannelUpdates(): Unit = {
    // populate new metadata node
    if (this.metadata.isEmpty) {
      this.metadata = Some(ApiChannelMetadataNew("system", lastModifiedDate))
    }

    // populate new fields node
    if (this.data.fields.isEmpty) {
      this.data = this.data.copy(fields = Some(this.data.metadata.data))
    }

    children.foreach(_.applyChannelUpdates())
  }

  override def equals(obj: Any): Boolean = obj match {
    case ApiChannel(otherId, _, _, _, _, _, _, _) ⇒ this.hashCode == otherId.hashCode
    case _ ⇒ false
  }

  override def hashCode: Int = this.id.hashCode
}

case class ChannelId(var path: String, isVirtual: Boolean = false, ece: Long = -1) {

  override def equals(obj: Any): Boolean = obj match {
    case ChannelId(_, _, otherEce) ⇒ this.ece.hashCode == otherEce.hashCode
    case _ ⇒ false
  }

  override def hashCode: Int = ece.hashCode

  // todo (mana): clean this up [2nd branch is not easy to underst
  def getLastPathPart : Option[String] = {
    path match {
      case "/" => Some("home")
      case channelPath if !channelPath.isEmpty ⇒ channelPath.split("/").filter(!_.isEmpty).lastOption
      case _ ⇒ None
    }
  }
}

case class ChannelUpdate(added: Seq[ApiChannel] = Seq.empty, deleted: Seq[ApiChannel] = Seq.empty, moved: Seq[ApiChannel] = Seq.empty) {
  def merge(other: ChannelUpdate): ChannelUpdate = ChannelUpdate(
    added = (added ++ other.added).distinct,
    deleted = (deleted ++ other.deleted).distinct,
    moved = (moved ++ other.moved).distinct
  )

  def isEmpty: Boolean = added.isEmpty && deleted.isEmpty && moved.isEmpty

  /** merge all the updates into this */
  def merge(updates: Seq[ChannelUpdate]): ChannelUpdate = updates.foldLeft(this)((acc, update) ⇒ acc.merge(update))
}

case class ApiChannelMetadataNew(changedBy: String = "system", lastModifiedDate: Long = Instant.now.toEpochMilli)

case class ApiChannelData(label: String,
                          adData: ApiChannelAdData = ApiChannelAdData(),
                          metadata: ApiChannelMetadata = ApiChannelMetadata(), // @deprecated
                          siteBuilding: Option[ApiChannelTheme] = None,
                          bgColor: Option[String] = None,
                          fields: Option[Map[String, String]] = None // todo, remove option when changes have been applied everywhere
                      )

case class ApiChannelMetadata(data: Map[String, String] = Map.empty)

case class ApiChannelAdData(definesAdTag: Boolean = false,
                            definesVideoAdTag: Option[Boolean] = None
                        )

case class ApiChannelTheme(theme: String = "default")

object ApiChannelTheme {
  lazy val DEFAULT = ApiChannelTheme(theme = "")

  lazy val ADVERTORIALS = ApiChannelTheme(theme = "advertorials")
  lazy val FORMEL1 = ApiChannelTheme(theme = "formel1")
  lazy val ICON = ApiChannelTheme(theme = "icon")
  lazy val MEDIATHEK = ApiChannelTheme(theme = "mediathek")
  lazy val NEWSTICKER = ApiChannelTheme(theme = "newsticker")
  lazy val OLYMPIA = ApiChannelTheme(theme = "olympia")

}
