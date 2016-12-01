package de.welt.contentapi.raw.models

import java.time.Instant

import play.api.libs.json.{JsError, JsResult, JsSuccess, JsValue, Json}

import scala.annotation.tailrec

/**
  * Tree structure of the escenic channel/section tree. Simple representation:
  * {{{
  * ┗━┓ '/' root (WON_frontpage)
  *   ┣━━━┓ '/sport/'
  *   ┃   ┣━━━━ '/sport/fussball/'
  *   ┃   ┣━━━━ '/sport/formel1/'
  *   ┃   ┗━━━━ '/sport/golf/'
  *   ┣━━━ '/wirtschaft/'
  *   ┗━━━ '/politik/'
  *
  * }}}
  *
  * @param id       mandatory id with the channel path. E.g. /sport/fussball/
  * @param config   channel configuration for the clients. Used by Funkotron.
  * @param stages   stage configuration for the channel. Used by Digger.
  * @param metadata meta data for CMCF and Janus. Needed for some merge/update/locking logic.
  * @param parent   the maybe parent of the current channel. Root channel has no parent. (no shit sherlock!)
  * @param children all children of the current channel
  */
case class RawChannel(id: RawChannelId,
                      var config: RawChannelConfiguration = RawChannelConfiguration(),
                      var stages: Option[Seq[RawChannelStage]] = None,
                      var metadata: RawMetadata = RawMetadata(),
                      var parent: Option[RawChannel] = None,
                      var children: Seq[RawChannel] = Nil,
                      var hasChildren: Boolean = false
                     ) {
  hasChildren = children.nonEmpty
  lazy val unwrappedStages: Seq[RawChannelStage] = stages.getOrElse(Nil)

  /**
    * @param search channel path. E.g. '/sport/fussball/'
    * @return maybe channel for the search string
    */
  def findByPath(search: String): Option[RawChannel] = findByPath(
    search.split('/').filter(_.nonEmpty).toList match {
      case Nil ⇒ Nil
      case head :: tail ⇒ tail.scanLeft(s"/$head/")((path, s) ⇒ path + s + "/")
    }
  )

  private def findByPath(sectionPath: Seq[String]): Option[RawChannel] = {
    sectionPath match {
      case Nil ⇒
        Some(this)
      case head :: Nil ⇒
        children.find(_.id.path == head)
      case head :: tail ⇒
        children.find(_.id.path == head).flatMap(_.findByPath(tail))
    }
  }

  final def findByEscenicId(escenicId: Long): Option[RawChannel] = {
    if (id.escenicId == escenicId) {
      Some(this)
    } else {
      children.flatMap { ch ⇒ ch.findByEscenicId(escenicId) }.headOption
    }
  }

  @tailrec
  final def root: RawChannel = parent match {
    case Some(p) ⇒ p.root
    case None ⇒ this
  }

  final def updateParentRelations(newParent: Option[RawChannel] = None): Unit = {
    this.parent = newParent
    children.foreach(_.updateParentRelations(Some(this)))
  }

  def getBreadcrumb: Seq[RawChannel] = {
    parent match {
      case None ⇒ this.copy(id = root.id.copy(label = "Home")) :: Nil
      case Some(p) ⇒ p.getBreadcrumb :+ this
    }
  }

  override def toString: String = s"Channel(id='${id.path}', ece=${id.escenicId}'')"

  /**
    * apply updates to this [[RawChannel]] from another [[RawChannel]] by overriding
    * the `path` and the `label` usually from ece sync
    *
    * @param other the source for the changes
    */
  def updateMasterData(other: RawChannel): Unit = {
    val needsUpdate = (id.path != other.id.path) || (id.label != other.id.label)
    id.path = other.id.path
    id.label = other.id.label
    if (needsUpdate) metadata = metadata.copy(lastModifiedDate = Instant.now.toEpochMilli)
  }

  /** equals solely on the ```ChannelId``` */
  override def equals(obj: Any): Boolean = obj match {
    case RawChannel(otherId, _, _, _, _, _, _) ⇒ this.hashCode == otherId.hashCode
    case _ ⇒ false
  }

  override def hashCode: Int = this.id.hashCode
}

/**
  * @param path      unique path of the channel. Always with a trailing slash. E.g. '/sport/fussball/'
  * @param label     label of the channel. This is the display name from escenic. (provided by SDP)
  * @param escenicId escenic id of the section. E.g. root channel ('/') with id 5. Default value '-1' is a error state.
  */
case class RawChannelId(var path: String,
                        var label: String,
                        escenicId: Long) {

  override def equals(obj: Any): Boolean = obj match {
    case RawChannelId(_, _, otherEce) ⇒ this.escenicId.hashCode == otherEce.hashCode
    case _ ⇒ false
  }

  override def hashCode: Int = escenicId.hashCode
}

/**
  * @param metadata   `<meta>` tag overrides of the channel.
  * @param header     content header (not the real page header) configuration.
  * @param theme      the optional theme for the channel. This is a developer configuration.
  * @param commercial commercial configuration for the channel. Used some override logic.
  */
case class RawChannelConfiguration(metadata: Option[RawChannelMetadata] = None,
                                   header: Option[RawChannelHeader] = None,
                                   theme: Option[RawChannelTheme] = None,
                                   commercial: RawChannelCommercial = RawChannelCommercial())

/**
  * The (ASMI) ad tag is a string with the root section and type of the page (section or content page).
  * When a channel defines an ad tag we override the root section with its own section.
  * We need this for some channel targeting. E.g. '/sport/formel1/' needs his own ad tag.
  *
  * @param definesAdTag      overrides the (ASMI) ad tag for the channel
  * @param definesVideoAdTag overrides the (ASMI) video ad tag for the channel
  */
case class RawChannelCommercial(definesAdTag: Boolean = false,
                                definesVideoAdTag: Boolean = false)

/**
  * What is a "Channel Theme"?
  * Think of a unique channel with:
  * - different background color
  * - other teaser
  * - new image crops
  * Example Channels:
  * - '/mediathek/'
  * - '/icon/'
  *
  * @param name   mapping string for the client. The impl. of the theme is part of the client (Funkotron)
  * @param fields optional settings/hints/values for the theme.
  */
case class RawChannelTheme(name: Option[String] = None, fields: Option[Map[String, String]] = None) {
  lazy val unwrappedFields: Map[String, String] = fields.getOrElse(Map.empty[String, String])
}

/**
  * @param title                     override `<title>` tag.
  * @param description               override `<meta name="description">` tag.
  * @param keywords                  override `<meta name="keywords">` tag.
  * @param sectionBreadcrumbDisabled `true` == no breadcrumb on the section page. The frontpage has no breadcrumb.
  * @param contentRobots             override `<meta name="robots">` tag only for all content pages of the channel.
  * @param sectionRobots             override `<meta name="robots">` tag only for the section page of the channel.
  */
case class RawChannelMetadata(title: Option[String] = None,
                              description: Option[String] = None,
                              keywords: Option[Seq[String]] = None,
                              sectionBreadcrumbDisabled: Option[Boolean] = None,
                              contentRobots: Option[RawChannelMetaRobotsTag] = None,
                              sectionRobots: Option[RawChannelMetaRobotsTag] = None) {
  lazy val unwrappedKeywords: Seq[String] = keywords.getOrElse(Nil)
}

/**
  * <meta name="robots" content="index,follow,noodp">
  *
  * @param noIndex  `true` == 'noIndex' & `false` == 'index'
  * @param noFollow `true` == 'noFollow' & `false` == 'follow'
  */
case class RawChannelMetaRobotsTag(noIndex: Option[Boolean] = None, noFollow: Option[Boolean] = None)

/**
  * Render a simple `<a>`.
  *
  * @param label display name of the link.
  * @param path  path (`href`) of the link.
  */
case class RawSectionReference(label: Option[String] = None, path: Option[String] = None)

/**
  *
  * @param sponsoring        only a mapping string for the client. Used for a svg/image logo. E.g. 'tagheuer'
  * @param logo              only a mapping string for the client. Used for a svg/image logo to replace the label.
  *                          The logo could be a channel logo like '/icon' or a ressort logo like '/kmpk'.
  *                          What's the different? Ask UI/UX!
  *                          Display-Logic:
  *                          channelLogo.orElse(ressortLogo).getOrElse(label)
  * @param slogan            slogan for the channel. E.g. /kmpkt: 'NEWS TO GO. EINZIGARTIG ANDERS.'
  * @param label             display name of the channel. The fallback label is always the [[RawChannelId.label]]
  * @param sectionReferences some optional links inside the header. Example: Link to a sub-channel.
  */
case class RawChannelHeader(sponsoring: Option[String] = None,
                            logo: Option[String] = None,
                            slogan: Option[String] = None,
                            label: Option[String] = None,
                            sectionReferences: Option[Seq[RawSectionReference]] = None) {
  lazy val unwrappedSectionReferences: Seq[RawSectionReference] = sectionReferences.getOrElse(Nil)
}

/**
  * Stored values for CMCF and Janus2. Should not be used by any clients.
  *
  * @param changedBy        github id of last sitebuilder
  * @param lastModifiedDate timestamp of last change
  */
case class RawMetadata(changedBy: String = "system",
                       lastModifiedDate: Long = Instant.now.toEpochMilli)


sealed trait RawChannelStage {
  val `type`: String
  val index: Int
  val hidden: Boolean
}

object RawChannelStage {

  val module = "module"
  val customModule = "custom-module"
  val commercial = "commercial"

  import de.welt.contentapi.raw.models.RawFormats.{rawChannelStageCommercialFormat, rawChannelStageContentFormat, rawChannelStageModuleFormat}
// this is only used by tests
  def apply(data: JsValue): RawChannelStage = {
    ((data \ "type").as[String] match {
      case RawChannelStage.customModule => Json.fromJson[RawChannelStageCustomModule](data)
      case RawChannelStage.module => Json.fromJson[RawChannelStageModule](data)
      case RawChannelStage.commercial => Json.fromJson[RawChannelStageCommercial](data)
    }) match {
      case JsSuccess(value, _) ⇒ value
      case JsError(err) ⇒ throw new IllegalArgumentException(err.toString())
    }
  }
}

/**
  * @param index          index of the stage (ordering)
  * @param module         identifier for the used Module, e.g. ChannelHero
  * @param labelOverride          display name of the stage
  * @param references     optional section references. Example: Link to Mediathek A-Z.
  * @param teaserLimitOverride    todo harry
  * @param sourceOverride the default source is always the current channel path. This is a override.
  * @param desktopLayoutOverride  mapping string for a (desktop) layout name. The mapping is for Digger and Clients.
  *                       Why desktop and not mobile?
  *                       On a mobile device all teasers inside a stage are among each another. Only the desktop
  *                       breakpoint need some 'hints' to structure the teasers. Example: 1/3 1/3 1/3 teaser row.
  */
case class RawChannelStageCustomModule(index: Int,
                                       module: String,
                                       hidden: Boolean = false,
                                       references: Option[Seq[RawSectionReference]] = None,
                                       labelOverride: Option[String] = None,
                                       teaserLimitOverride: Option[Int] = None,
                                       sourceOverride: Option[String] = None,
                                       desktopLayoutOverride: Option[String] = None,
                                       `type`: String = RawChannelStage.customModule
                                       ) extends RawChannelStage {
  lazy val unwrappedReferences: Seq[RawSectionReference] = references.getOrElse(Nil)
}

/**
  * todo harry: WTF is a module?
  *
  * @param index       index of the stage (ordering)
  * @param module      name used for matching existing Modules in Digger
  */
case class RawChannelStageModule(index: Int,
                                 module: String,
                                 hidden: Boolean = false,
                                 `type`: String = RawChannelStage.module
                                 ) extends RawChannelStage {
}

/**
  * @param index      index of the stage (ordering)
  * @param format     identifier of Advertorial, e.g. Billboard
  */
case class RawChannelStageCommercial(index: Int, format: String, hidden: Boolean = false,`type`: String = RawChannelStage.commercial) extends RawChannelStage {
}

