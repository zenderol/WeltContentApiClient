package de.welt.contentapi.raw.models

import java.time.Instant

import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}

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
  * @param id                 mandatory id with the channel path. E.g. /sport/fussball/
  * @param config             channel configuration for the clients. Used by Funkotron.
  * @param stageConfiguration Object that either holds a template name or a seq of manually configured Stages. Used by Digger.
  * @param metadata           meta data for CMCF and Janus. Needed for some merge/update/locking logic.
  * @param parent             the maybe parent of the current channel. Root channel has no parent. (no shit sherlock!)
  * @param children           all children of the current channel
  * @param hasChildren        flag if channel has children
  */
case class RawChannel(id: RawChannelId,
                      var config: RawChannelConfiguration = RawChannelConfiguration(),
                      var stageConfiguration: Option[RawChannelStageConfiguration] = None,
                      var metadata: RawMetadata = RawMetadata(),
                      var parent: Option[RawChannel] = None,
                      var children: Seq[RawChannel] = Nil,
                      var hasChildren: Boolean = false) {
  hasChildren = children.nonEmpty
  lazy val unwrappedStages: Seq[RawChannelStage] = stageConfiguration.flatMap(_.stages).getOrElse(Nil)

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

  /**
    * Find inherited or self configured overrides for ApiContentSearches
    */
  def getMaybeContentOverrides: Option[RawChannelContentConfiguration] = parent match {
    // has own config
    case _ if this.config.content.isDefined ⇒ this.config.content
    // has a parent with config -> use parent's config
    case Some(configuredParent) if configuredParent.config.content.isDefined ⇒ configuredParent.config.content
    //  go up in tree one level and retry
    case Some(notConfiguredParent) ⇒ notConfiguredParent.getMaybeContentOverrides
    // nothing found -> None
    case _ ⇒ None
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

  /**
    * set the same RawChannelStageConfiguration for all Sub-Channels but not the channel itself
    *
    * @param newStageConfig New config to inherit
    */
  def batchInheritRawChannelStageConfigurationToAllChildren(newStageConfig: RawChannelStageConfiguration, user: String): Unit =
    batchInheritGenericToAllChildren({_.stageConfiguration = Some(newStageConfig)}, user)

  /**
    * set the same RawChannelSponsoring for all Sub-Channels but not the channel itself
    *
    * @param newSponsoring New Sponsoring to inherit
    */
  def batchInheritRawChannelSponsoringToAllChildren(newSponsoring: RawChannelSponsoring, user: String): Unit =
    batchInheritGenericToAllChildren({ rawChannel ⇒ rawChannel.config = rawChannel.config.copy(sponsoring = newSponsoring) }, user)

  /**
    * set the same RawChannelTheme for all Sub-Channels but not the channel itself
    *
    * @param newTheme New RawChannelTheme to inherit
    */
  def batchInheritRawChannelThemeToAllChildren(newTheme: RawChannelTheme, user: String): Unit =
    batchInheritGenericToAllChildren({ rawChannel ⇒ rawChannel.config = rawChannel.config.copy(theme = Some(newTheme)) }, user)

  /**
    * set the same RawChannelHeader for all Sub-Channels but not the channel itself
    *
    * @param newHeader New header to inherit
    */
  def batchInheritRawChannelHeaderToAllChildren(newHeader: RawChannelHeader, user: String): Unit =
    batchInheritGenericToAllChildren({ rawChannel ⇒ rawChannel.config = rawChannel.config.copy(header = Some(newHeader)) }, user)

  /**
    * set the same RawChannelTaboolaCommercial for all Sub-Channels but not the channel itself
    *
    * @param newTaboolaConfig New header to inherit
    */
  def batchInheritRawChannelTaboolaCommercialToAllChildren(newTaboolaConfig: RawChannelTaboolaCommercial, user: String): Unit = {
    batchInheritGenericToAllChildren((rawChannel: RawChannel) ⇒ rawChannel.config.commercial.contentTaboola = newTaboolaConfig, user)
  }

  private[models] def batchInheritGenericToAllChildren(applyInheritanceAction: RawChannel ⇒ Unit,
                                                       user: String,
                                                       timestamp: Long = Instant.now.toEpochMilli): Unit = this.children.foreach { child ⇒
    applyInheritanceAction(child)
    child.metadata = child.metadata.copy(changedBy = user, lastModifiedDate = timestamp)
    // enter recursion
    child.batchInheritGenericToAllChildren(applyInheritanceAction, user, timestamp)
  }
}

/**
  * Wrapper object. Either manually configured stages or use a template for the channel like "default" or "mediathek"
  *
  * @param stages       manually configured stages from CMCF. Used in Digger.
  * @param templateName name of the template to use instead of manually configured stages. Used in Digger.
  */
case class RawChannelStageConfiguration(stages: Option[Seq[RawChannelStage]] = None,
                                        templateName: Option[String] = None)

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
  * @param sponsoring sponsoring mapping configuration for the channel.
  * @param theme      the optional theme for the channel. This is a developer configuration.
  * @param commercial commercial configuration for the channel. Used some override logic.
  * @param content    content query configuration for the whole channel and all sub-channel (children).
  * @param brand      flags a channel and all sub-channels (children) as a 'brand'. A brand is a "Sub-Marke"
  *                   like Icon ('/icon/') with different UI elements or layouts.
  */
case class RawChannelConfiguration(metadata: Option[RawChannelMetadata] = None,
                                   header: Option[RawChannelHeader] = None,
                                   sponsoring: RawChannelSponsoring = RawChannelSponsoring(),
                                   theme: Option[RawChannelTheme] = None,
                                   commercial: RawChannelCommercial = RawChannelCommercial(),
                                   content: Option[RawChannelContentConfiguration] = None,
                                   brand: Boolean = false)

/**
  * The (ASMI) ad tag is a string with the root section and type of the page (section or content page).
  * When a channel defines an ad tag we override the root section with its own section.
  * We need this for some channel targeting. E.g. '/sport/formel1/' needs his own ad tag.
  *
  * @param definesAdTag      Overrides the (ASMI) ad tag for the channel
  * @param definesVideoAdTag Overrides the (ASMI) video ad tag for the channel
  * @param contentTaboola    Controls Taboola commercials on all content pages of the channel.
  */
case class RawChannelCommercial(definesAdTag: Boolean = false,
                                definesVideoAdTag: Boolean = false,
                                var contentTaboola: RawChannelTaboolaCommercial = RawChannelTaboolaCommercial())

/**
  * Enable/Disable Taboola scripts on content pages below the article text. Some Channel do not want
  * all Taboola scripts -- e.g. /icon/
  *
  * @param showNews    "Mehr aus dem Web". Taboola named it 'Below Article Thumbnails'
  * @param showWeb     "Neues aus der Redaktion". Taboola named it 'Below Article Thumbnails 2nd'
  * @param showNetwork "Neues aus unserem Netzwerk". Taboola named it 'Exchange Below Article Thumbnails'
  */
case class RawChannelTaboolaCommercial(showNews: Boolean = true,
                                       showWeb: Boolean = true,
                                       showNetwork: Boolean = true)

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
  * @param logo              only a mapping string for the client. Used for a svg/image logo to replace the label.
  *                          Display-Logic: `logo.getOrElse(label)`
  * @param slogan            slogan for the channel. E.g. /kmpkt: 'NEWS TO GO. EINZIGARTIG ANDERS.'
  * @param label             display name of the channel. The fallback label is always the [[RawChannelId.label]]
  * @param sectionReferences some optional links inside the header. Example: Link to a sub-channel.
  * @param hidden            hide only the channel header. (Not affected: Sponsoring, References). Default = `false`
  * @param adIndicator       Indicator for an advertorial or mark as advertisement. Used for: display the label 'Anzeige'.
  *                          Default = `false`
  */
case class RawChannelHeader(logo: Option[String] = None,
                            slogan: Option[String] = None,
                            label: Option[String] = None,
                            sectionReferences: Option[Seq[RawSectionReference]] = None,
                            hidden: Boolean = false,
                            adIndicator: Boolean = false) {
  lazy val unwrappedSectionReferences: Seq[RawSectionReference] = sectionReferences.getOrElse(Nil)
}

/**
  * A channel can be sponsored by a partner or brand with a special logo + slogan. This is mostly part of the
  * page-sub-header.
  *
  * @param logo   only a mapping string for the client. Used for a svg/image logo e.g. 'Commerzbank' or 'Philips'
  * @param slogan partner slogan for the channel sponsoring.
  *               E.g. "Philips - Es gibt immer einen Weg, das Leben besser zu machen"
  * @param hidden hide only the sponsoring. Default = `false`
  */
case class RawChannelSponsoring(logo: Option[String] = None, slogan: Option[String] = None, hidden: Boolean = false)

/**
  * Stored values for CMCF and Janus2. Should not be used by any clients.
  *
  * @param changedBy        github id of last sitebuilder
  * @param lastModifiedDate timestamp of last change
  */
case class RawMetadata(changedBy: String = "system",
                       lastModifiedDate: Long = Instant.now.toEpochMilli)

/**
  * Some sections only contain content with particular subTypes or Types e.g. "/regionales/" have subType "ticker"
  *
  * @param subTypeQueryForText  SubTypes used for stages with text articles e.g. "-ticker,-live" or "ticker"
  * @param typeQueryForText     Types used for stages with text articles e.g. "article"
  * @param subTypeQueryForVideo SubTypes used for stages with video articles e.g. "video,broadcast"
  * @param typeQueryForVideo    Types used for stages with video articles e.g. "video"
  */
case class RawChannelContentConfiguration(subTypeQueryForText: Option[String] = None,
                                          typeQueryForText: Option[String] = None,
                                          subTypeQueryForVideo: Option[String] = None,
                                          typeQueryForVideo: Option[String] = None) {
}

/**
  * type is the the identifier for case class matching
  * index is for sorting the stages according to cmcf order
  * hidden allows stages to be configured but not the be rendered
  */
sealed trait RawChannelStage {
  val `type`: String
  val index: Int
  val hidden: Boolean
}

object RawChannelStage {
  val module = "module"
  val customModule = "custom-module"
  val commercial = "commercial"
  val curated = "curated"
  val unknown = "unknown"

  import de.welt.contentapi.raw.models.RawFormats.{rawChannelStageCommercialFormat, rawChannelStageContentFormat, rawChannelStageCuratedFormat}

  // this is only used by tests
  def apply(data: JsValue): RawChannelStage = {
    ((data \ "type").as[String] match {
      case RawChannelStage.customModule => Json.fromJson[RawChannelStageCustomModule](data)
      case RawChannelStage.module => Json.fromJson[RawChannelStageCustomModule](data) // getting rid of the "module" to simplify cmcf ui
      case RawChannelStage.commercial => Json.fromJson[RawChannelStageCommercial](data)
      case RawChannelStage.curated => Json.fromJson[RawChannelStageCurated](data)
    }) match {
      case JsSuccess(value, _) ⇒ value
      case JsError(err) ⇒ throw new IllegalArgumentException(err.toString())
    }
  }
}

/**
  * A module is a stage template, consisting of content search and layout configuration
  * The RawChannelStageModule was removed to simplify the CMCF UI.
  * Every module identifier leads to a default configuration for content and layout
  * Overrides for the defaults can be done in CMCF, see possible values below at the overrides parameter
  *
  * @param module     identifier for the used Module, e.g. ChannelHero
  * @param references optional section references. Example: Link to Mediathek A-Z.
  * @param overrides  optional overrides for the Stage, e.g. type, subType, sectionPath
  *                   Currently allowed/mapped values are: `sectionPath`, `limit`, `layout`, `label`
  */
case class RawChannelStageCustomModule(index: Int,
                                       module: String,
                                       hidden: Boolean = false,
                                       references: Option[Seq[RawSectionReference]] = None,
                                       overrides: Option[Map[String, String]] = None,
                                       `type`: String = RawChannelStage.customModule
                                      ) extends RawChannelStage {
  lazy val unwrappedReferences: Seq[RawSectionReference] = references.getOrElse(Nil)
}

/**
  * A RawChannelStageCommercial is a stage only consisting of a commercial.
  * Before commercials were attached to other stages. When the stages had no content the commercial wasn't rendered as well.
  * Today we have the commercials separately to always render the commercials independently from the content.
  *
  * @param index  index of the stage (ordering)
  * @param format identifier of Advertorial, e.g. Billboard
  */
case class RawChannelStageCommercial(index: Int, format: String, hidden: Boolean = false, `type`: String = RawChannelStage.commercial) extends RawChannelStage {
}


/**
  * Curated Stage to be configured in CMCF.
  * Allows placing curated Stages from Papyrus on Channels
  *
  * @param label optional label to be rendered above the stage, e.g. name of channel
  * @param layout optional layout name to be used for the stage, e.g. "classic-ressort" else will be default layout
  * @param curatedSectionMapping the id of the curated section in Papyrus, e.g. "frontpage" or "icon"
  * @param curatedStageMapping the id of the curated stage within a curated section, e.g. "sport", "uhren", or "iconist"
  */
case class RawChannelStageCurated(index: Int,
                                  label: Option[String],
                                  layout: Option[String],
                                  curatedSectionMapping: String,
                                  curatedStageMapping: String,
                                  hidden: Boolean = false,
                                  references: Option[Seq[RawSectionReference]] = None,
                                  `type`: String = RawChannelStage.curated) extends RawChannelStage {
  lazy val unwrappedReferences: Seq[RawSectionReference] = references.getOrElse(Nil)

}

/**
  * Unknown Modules will be parsed as [[RawChannelStageIgnored]] for future-proof Json Parsing
  * Use Case: CMCF can be rolled out with new Modules that are not yet known to Digger
  */
case class RawChannelStageIgnored(index: Int) extends RawChannelStage {
  override val `type`: String = RawChannelStage.unknown
  override val hidden: Boolean = true
}

