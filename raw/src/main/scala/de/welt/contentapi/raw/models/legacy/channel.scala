package de.welt.contentapi.raw.models.legacy

import java.time.Instant

import org.slf4j.LoggerFactory

import scala.annotation.tailrec

case class ApiChannel(id: ChannelId,
                      var data: ApiChannelData,
                      var stages: Option[Seq[Stage]] = None,
                      var parent: Option[ApiChannel] = None,
                      var children: Seq[ApiChannel] = Seq.empty,
                      var hasChildren: Boolean = false,
                      var lastModifiedDate: Long = Instant.now.toEpochMilli,
                      var metadata: Option[ApiChannelMetadataNew] = None) {


  private final val log = LoggerFactory.getLogger(getClass.getName.stripSuffix("$"))
  hasChildren = children.nonEmpty
  val DEFAULT_AD_TAG = "sonstiges"


  final def updateParentRelations(newParent: Option[ApiChannel] = None): Unit = {
    this.parent = newParent
    children.foreach(_.updateParentRelations(Some(this)))
  }


  def getParentSafeley(channel: ApiChannel): Option[ApiChannel] = {
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
  def getLastPathPart: Option[String] = {
    path match {
      case "/" => Some("home")
      case channelPath if !channelPath.isEmpty ⇒ channelPath.split("/").filter(!_.isEmpty).lastOption
      case _ ⇒ None
    }
  }
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
