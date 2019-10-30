package de.welt.contentapi.raw.models

import java.time.Instant
import java.util.function.Predicate

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

  /**
    * Recursive function which returns all channels, that match the Predicate
    *
    * @param p the Predicate[RawChannel] to test for
    * @return all matching Channels
    */
  final def findByPredicate(p: Predicate[RawChannel]): Seq[RawChannel] = {
    val maybeMatch: Seq[RawChannel] = if (p.test(this)) Seq(this) else Seq.empty
    maybeMatch ++ children.flatMap { ch: RawChannel ⇒ ch.findByPredicate(p) }
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
    updateMasterData(other.id.path, other.id.label)
  }

  def updateMasterData(newPath: String, newLabel: String): Unit = {
    val needsUpdate = (id.path != newPath) || (id.label != newLabel)
    id.path = newPath
    id.label = newLabel
    if (needsUpdate) metadata = metadata.copy(lastModifiedDate = Instant.now.toEpochMilli)
  }

  /** equals solely on the ```ChannelId``` */
  override def equals(obj: Any): Boolean = obj match {
    case RawChannel(otherId, _, _, _, _, _, _) ⇒ this.id.hashCode == otherId.hashCode
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
  def batchInheritRawChannelSponsoringToAllChildren(newSponsoring: RawSponsoringConfig, user: String): Unit =
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
  * @param metadata          `<meta>` tag overrides of the channel.
  * @param header            content header (not the real page header) configuration.
  * @param sponsoring        sponsoring mapping configuration for the channel.
  * @param siteBuilding      customization of header, footer and channel sponsoring.
  * @param theme             the optional theme for the channel. This is a developer configuration.
  * @param commercial        commercial configuration for the channel. Used some override logic.
  * @param articlePromotions escenic articles that should be put as inline elements, configured in CMCF
  * @param content           content query configuration for the whole channel and all sub-channel (children).
  * @param brand             flags the channel and all sub-channels (children) as a 'brand'. A brand is a "Sub-Marke"
  *                          like Icon ('/icon/') with different UI elements or layouts.
  * @param master            flags the channel as a 'master' channel. All it's sub-channels (children) get this channel
  *                          as it's master. E.g. `/wirtschaft/bilanz/` is flagged as a master channel
  */
case class RawChannelConfiguration(metadata: Option[RawChannelMetadata] = None,
                                   @deprecated("Use siteBuilding instead", since = "version 2.3")
                                   header: Option[RawChannelHeader] = None,
                                   @deprecated("Use siteBuilding instead", since = "version 2.3")
                                   sponsoring: RawSponsoringConfig = RawSponsoringConfig(),
                                   siteBuilding: Option[RawChannelSiteBuilding] = None,
                                   theme: Option[RawChannelTheme] = None,
                                   commercial: RawChannelCommercial = RawChannelCommercial(),
                                   content: Option[RawChannelContentConfiguration] = None,
                                   articlePromotions: Option[Seq[RawArticlePromotion]] = None,
                                   brand: Boolean = false,
                                   master: Boolean = false)

/**
  * Reference to another content which has to be promoted inside the article
  *
  * @param contentId   CMS ID of the promoted content
  * @param `type`      Specifies the allowed content type of the promotion
  */
case class RawArticlePromotion(contentId: String, `type`: String)

/**
  * The (ASMI) ad tag is a string with the root section and type of the page (section or content page).
  * When a channel defines an ad tag we override the root section with its own section.
  * We need this for some channel targeting. E.g. '/sport/formel1/' needs his own ad tag.
  *
  * @param definesAdTag         Overrides the (ASMI) ad tag for the channel
  * @param definesVideoAdTag    Overrides the (ASMI) video ad tag for the channel
  * @param contentTaboola       Controls Taboola commercials on all content pages of the channel.
  * @param showFallbackAds      Control to display fallback ads if ASMI fails to deliver their own for several ad formats (m-rectangle, skyscraper, ...)
  * @param disableAdvertisement Disable all advertisements for this channel; does not inherit to children (#akalies)
  */
case class RawChannelCommercial(definesAdTag: Boolean = false,
                                definesVideoAdTag: Boolean = false,
                                var contentTaboola: RawChannelTaboolaCommercial = RawChannelTaboolaCommercial(),
                                showFallbackAds: Boolean = true,
                                disableAdvertisement: Boolean = false)

/**
  * Enable/Disable Taboola scripts on content pages below the article text. Some Channel do not want
  * all Taboola scripts -- e.g. /icon/
  *
  * @param showNews        "Mehr aus dem Web". Taboola named it 'Below Article Thumbnails'
  * @param showWeb         "Neues aus der Redaktion". Taboola named it 'Below Article Thumbnails 2nd'
  * @param showWebExtended "Auch interessant". Taboola named it 'Below Article Thumbnails 3rd'
  * @param showNetwork     "Neues aus unserem Netzwerk". Taboola named it 'Exchange Below Article Thumbnails'
  */
case class RawChannelTaboolaCommercial(showNews: Boolean = true,
                                       showWeb: Boolean = true,
                                       showWebExtended: Boolean = true,
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
  * @param label             display name of the channel. The fallback label is always the [[RawChannelId.label]]
  * @param logo              only a mapping string for the client. Used for a svg/image logo to replace the label.
  *                          Display-Logic: `logo.getOrElse(label)`
  * @param headerReference   Optional link for the logo/label.
  *                          Link logic (pseudo-code):  `headerReference.getOrElse(Master-Channel-Link)`
  * @param slogan            slogan for the channel. E.g. /kmpkt/: 'NEWS TO GO. EINZIGARTIG ANDERS.'
  * @param sloganReference   Optional link for the slogan.
  * @param sectionReferences Used as a sub-navigation to make sub-channels reachable
  * @param hidden            hide only the channel header. (Not affected: Sponsoring, References). Default = `false`
  * @param adIndicator       Indicator for an advertorial or mark as advertisement. Used for: display the label 'Anzeige'.
  *                          Default = `false`
  */
case class RawChannelHeader(label: Option[String] = None,
                            logo: Option[String] = None,
                            headerReference: Option[RawSectionReference] = None,
                            slogan: Option[String] = None,
                            sloganReference: Option[RawSectionReference] = None,
                            sectionReferences: Option[Seq[RawSectionReference]] = None, // == SubNavi
                            hidden: Boolean = false, // => complete clean header
                            adIndicator: Boolean = false) {

  lazy val unwrappedSectionReferences: Seq[RawSectionReference] = sectionReferences.getOrElse(Nil)

  /**
    * isEmpty / nonEmpty are used for inheritance decisions
    *
    * because hidden and adIndicator are not optional fields the RawChannelHeader object is always `Some()`
    * Some(RawChannelHeader) may still be counted as empty if
    *   a) its values are equal to the Constructor defaults
    *   b) only adIndicator is set to true
    *      the field is wrongly placed here, because it is only used for the Commercial Configuration
    */
  lazy val isEmpty: Boolean = this == RawChannelHeader() || this == RawChannelHeader(adIndicator = true)
  lazy val nonEmpty: Boolean = !isEmpty
}

/**
  * A channel or a stage can be sponsored by a partner or brand with a special logo + slogan. This is mostly part of the
  * page-sub-header.
  *
  * @param logo         only a mapping string for the client. Used for a svg/image logo e.g. 'Commerzbank' or 'Philips'
  * @param slogan       partner slogan for the channel sponsoring.
  *               E.g. "Philips - Es gibt immer einen Weg, das Leben besser zu machen"
  * @param hidden       hide only the sponsoring. Default = `false`
  * @param link         Optional link for the logo.
  * @param brandstation Optional type of Brandstation if the partner is part of brandstation.
  */
case class RawSponsoringConfig(logo: Option[String] = None,
                               slogan: Option[String] = None,
                               hidden: Boolean = false,
                               link: Option[RawSectionReference] = None,
                               brandstation: Option[String] = None)

/**
  * Channel Site-Building. Configure Header, Footer and Sponsoring
  *
  * @param fields               optional settings/values for the channel i.e. custom footer settings, labels, logos.
  * @param sub_navigation       optional section references. Example: Link to Mediathek A-Z.
  * @param elements             configurable media element containing URLs (images) from Escenic.
  */
case class RawChannelSiteBuilding(fields: Option[Map[String, String]] = None,
                                  sub_navigation: Option[Seq[RawSectionReference]] = None,
                                  elements: Option[Seq[RawElement]] = None) {

  def unwrappedSubNavigation: Seq[RawSectionReference] = sub_navigation.getOrElse(Nil)
  def unwrappedElements: Seq[RawElement] = elements.getOrElse(Nil)
  def unwrappedFields: Map[String, String] = nonEmptyFields.getOrElse(Map.empty)

  def nonEmptyFields: Option[Map[String, String]] = fields.map(x => x.filterNot(v => v._2.isBlank).filterNot(v => v._1 == RawReads.MigrationHintFieldName))

  def isEmpty: Boolean = this == RawChannelSiteBuilding() || this == RawChannelSiteBuilding(fields = Some(Map.empty[String, String]))

  // needed for inheritance, see Raw2Api Converter merge logic
  def headerFields: Map[String, String] = fieldsWithPrefix("header_")
  def sponsoringFields: Map[String, String] = fieldsWithPrefix("sponsoring_")
  def partnerFields: Map[String, String] = fieldsWithPrefix("partner_")
  def footerFields: Map[String, String] = fieldsWithPrefix("footer_")
  def generalFields: Map[String, String] = fieldsWithPrefix("general_")

  // `header_hidden` comes from CMCF internal state, where hidden can only be `true` of `false`, but will never be undefined or missing
  def emptyHeader: Boolean = headerFields == Map("header_hidden" -> "false") || headerFields.isEmpty
  def emptySponsoring: Boolean = sponsoringFields == Map("sponsoring_hidden" -> "false") || sponsoringFields.isEmpty
  def emptyPartner: Boolean = partnerFields == Map("partner_header_hidden" -> "false") || partnerFields.isEmpty
  def emptyFooter: Boolean = footerFields == Map("footer_hidden" -> "false") || footerFields.isEmpty
  def emptyGeneral: Boolean = generalFields.isEmpty
  def emptySubNavi: Boolean = unwrappedSubNavigation.isEmpty
  def emptyElements: Boolean = unwrappedElements.isEmpty

  private def fieldsWithPrefix(prefix: String) = {
    this.unwrappedFields.filter(v => v._1.startsWith(prefix))
  }

  // if anything is empty, look for Master, that may inherit some values
  def isMasterInheritanceEligible: Boolean = emptyHeader || emptySponsoring || emptySubNavi || emptyElements || emptyFooter || emptyPartner || emptyGeneral
}

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
  * index is for sorting the stages according to cmcf order
  * `type` is the identifier for case class matching
  * hidden allows stages to be configured but not rendered
  * trackingName is used by Funkotron for tracking clicks on articles in stages (e.g. Webtrekk - important for Editors and BI!)
  * link is used by Funkotron for linking the stage header label (e.g. Welt+ stage links to //www.welt.de/weltplus/ channel)
  */
sealed trait RawChannelStage {
  def index: Int

  def `type`: String

  def hidden: Boolean

  def trackingName: Option[String]

  def link: Option[RawSectionReference]

  def hasType(typ: String): Boolean = typ == `type`
}

/**
  * This is the model for God-Mode Stages from CMCF
  * (The RawChannelStageModule was removed to be able to simplify the CMCF UI.)
  * The value for `module` leads to a configuration for content and layout
  * Overrides for the defaults can be done in CMCF, see possible values below at the overrides parameter
  *
  * @param module     identifier for the referenced Module, e.g. ChannelHero
  * @param references optional section references. Example: Link to Mediathek A-Z.
  * @param overrides  optional overrides for the Stage, e.g. type, subType, sectionPath
  *                   Currently allowed/mapped values are: `sectionPath`, `limit`, `layout`, `label`
  * @param logo       optional logo to be rendered next to the label, e.g. gruenderszene.de stage logo.
  */
case class RawChannelStageCustomModule(override val index: Int,
                                       override val `type`: String = RawChannelStage.TypeCustomModule,
                                       override val hidden: Boolean = RawChannelStage.HiddenDefault,
                                       override val trackingName: Option[String],
                                       override val link: Option[RawSectionReference],
                                       module: String,
                                       references: Option[Seq[RawSectionReference]] = None,
                                       overrides: Option[Map[String, String]] = None,
                                       logo: Option[String] = None) extends RawChannelStage {
  lazy val unwrappedReferences: Seq[RawSectionReference] = references.getOrElse(Nil)
  lazy val unwrappedOverrides: Map[String, String] = overrides.getOrElse(Map.empty[String, String])
}

/**
  * A RawChannelStageCommercial is a stage only consisting of a commercial.
  * In earlier days commercials were only attached to other stages.
  * When the stages had no content the commercial wasn't rendered as well.
  * Today we have the commercials separately to always render the commercials independently from the content.
  *
  * @param format identifier of Advertorial, e.g. Billboard
  */
case class RawChannelStageCommercial(override val index: Int,
                                     override val `type`: String = RawChannelStage.TypeCommercial,
                                     override val hidden: Boolean = RawChannelStage.HiddenDefault,
                                     override val trackingName: Option[String],
                                     override val link: Option[RawSectionReference],
                                     format: String) extends RawChannelStage {
}


/**
  * Curated Stage to be configured in CMCF.
  * Allows placing curated Stages from Papyrus on Channels
  *
  * @param curatedSectionMapping the id of the curated section in Papyrus, e.g. "frontpage" or "icon"
  * @param curatedStageMapping   the id of the curated stage within a curated section, e.g. "sport", "uhren", or "iconist"
  * @param layout                optional layout name to be used for the stage, e.g. "classic-ressort" else will be default layout
  * @param label                 optional label to be rendered above the stage, e.g. name of channel
  * @param logo                  optional logo to be rendered next to the label, e.g. `/icon/` stage logos.
  * @param sponsoring            optional sponsoring consisting of a linked logo and/or slogan
  * @param references            optional Link(s) to external or internal, absolute or relative URLs
  * @param hideCuratedStageLabel don't show the label that curation api returns (allow re-usage of stages)
  */
case class RawChannelStageCurated(override val index: Int,
                                  override val `type`: String = RawChannelStage.TypeCurated,
                                  override val hidden: Boolean = RawChannelStage.HiddenDefault,
                                  override val trackingName: Option[String],
                                  override val link: Option[RawSectionReference],
                                  curatedSectionMapping: String,
                                  curatedStageMapping: String,
                                  layout: Option[String],
                                  label: Option[String],
                                  logo: Option[String],
                                  sponsoring: Option[RawSponsoringConfig] = None,
                                  references: Option[Seq[RawSectionReference]] = None,
                                  hideCuratedStageLabel: Option[Boolean] = None) extends RawChannelStage {
  lazy val unwrappedReferences: Seq[RawSectionReference] = references.getOrElse(Nil)

  def hasValues(section: String, stage: String) = this.curatedSectionMapping == section && this.curatedStageMapping == stage

}

/**
  * This stage is a way to curate an escenic id on a section page without needing Papyrus Curation
  * The intended usage is to place Advertorial Footers or the occasional WM OEmbeds on Section Pages
  *
  * @param configuredId An escenic Id to be resolved by the Section Backend
  * @param label optional label to be rendered above the stage, e.g. name of channel
  * @param description optional description which is used only in CMCF to visualize the meaning behind the ID
  * @param references optional link(s) to external or internal, absolute or relative URLs
  */
case class RawChannelStageConfiguredId(override val index: Int,
                                  override val `type`: String = RawChannelStage.TypeConfiguredId,
                                  override val hidden: Boolean = RawChannelStage.HiddenDefault,
                                  override val trackingName: Option[String],
                                  override val link: Option[RawSectionReference],
                                  configuredId: String,
                                  label: Option[String],
                                  description: Option[String],
                                  references: Option[Seq[RawSectionReference]] = None) extends RawChannelStage {
  lazy val unwrappedReferences: Seq[RawSectionReference] = references.getOrElse(Nil)

}

/**
  * Stage that (currently) represents a Webtrekk Report, e.g. Most-Read
  *
  * @param reportName the name as configured in Webtrekk, should not contain Whitespaces
  */
case class RawChannelStageTracking(override val index: Int,
                                   override val `type`: String = RawChannelStage.TypeTracking,
                                   override val hidden: Boolean = RawChannelStage.HiddenDefault,
                                   override val trackingName: Option[String],
                                   override val link: Option[RawSectionReference],
                                   layout: Option[String],
                                   label: Option[String],
                                   logo: Option[String],
                                   references: Option[Seq[RawSectionReference]] = None,
                                   reportName: String) extends RawChannelStage {
  lazy val unwrappedReferences: Seq[RawSectionReference] = references.getOrElse(Nil)
}

/**
  * Unknown Modules will be parsed as [[RawChannelStageIgnored]] for future-proof Json Parsing
  * Use Case: CMCF can be rolled out with new Modules that are not yet known to Digger
  */
case class RawChannelStageIgnored(override val index: Int,
                                  override val `type`: String = RawChannelStage.TypeUnknown,
                                  override val hidden: Boolean = true,
                                  override val trackingName: Option[String] = None,
                                  override val link: Option[RawSectionReference] = None) extends RawChannelStage


object RawChannelStage {
  val HiddenDefault = false
  val TypeModule = "module"
  val TypeCustomModule = "custom-module"
  val TypeCommercial = "commercial"
  val TypeCurated = "curated"
  val TypeConfiguredId = "configured-id"
  val TypeTracking = "tracking"
  val TypeUnknown = "unknown"
}


object RawChannelElement {
  val IdDefault = "channel_element"
  val TypeUnknown = "unknown"
}

object RawChannelAsset {
  val TypeUnknown = "unknown"
}