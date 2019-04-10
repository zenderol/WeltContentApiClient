package de.welt.contentapi.pressed.client.converter

import com.google.inject.Singleton
import de.welt.contentapi.core.models.{ApiAsset, ApiElement, ApiReference}
import de.welt.contentapi.pressed.models._
import de.welt.contentapi.raw.models.{RawChannel, RawChannelCommercial, RawChannelMetaRobotsTag}
import javax.inject.Inject


@Singleton
class RawToApiConverter @Inject()(inheritanceCalculator: InheritanceCalculator) {
  private val pathForAdTagAction: InheritanceAction[String] = InheritanceAction[String](
    forRoot = _ ⇒ "home", // root has a unique adTag
    forFallback = _ ⇒ "sonstiges", // fallback value for First-Level-Sections with no own adTag
    forMatching = c ⇒ trimPathForAdTag(c.id.path)
  )

  /**
    * Converter method that takes a rawChannel and returns an ApiChannel from its data
    *
    * @param rawChannel the rawChannel produced by ConfigMcConfigFace
    * @return a new ApiChannel with the data from the rawChannel
    */
  def apiChannelFromRawChannel(rawChannel: RawChannel): ApiChannel = {
    ApiChannel(
      section = Some(getApiSectionReferenceFromRawChannel(rawChannel)),
      master = calculateMasterReference(rawChannel),
      breadcrumb = Some(getBreadcrumb(rawChannel)),
      brand = Some(calculateBrand(rawChannel))
    )
  }

  private[converter] def getApiSectionReferenceFromRawChannel(rawChannel: RawChannel): ApiReference = {
    ApiReference(
      label = Some(rawChannel.id.label),
      href = Some(rawChannel.id.path)
    )
  }

  private[converter] def getBreadcrumb(raw: RawChannel): Seq[ApiReference] = raw.getBreadcrumb.map(b ⇒ ApiReference(Some(b.id.label), Some(b.id.path)))

  /**
    * Converter method that takes a rawChannel and returns an ApiConfiguration from its data or ancestors
    *
    * @param rawChannel the rawChannel produced by ConfigMcConfigface
    * @return an ApiConfiguration Object with all necessary data
    */
  def apiConfigurationFromRawChannel(rawChannel: RawChannel) = ApiConfiguration(
    meta = apiMetaConfigurationFromRawChannel(rawChannel),
    commercial = Some(apiCommercialConfigurationFromRawChannel(rawChannel)),
    sponsoring = Some(apiSponsoringConfigurationFromRawChannel(rawChannel)),
    header = calculateHeader(rawChannel),
    theme = calculateTheme(rawChannel),
    siteBuilding = calculateSiteBuilding(rawChannel)
  )

  //todo: return Option[] (no hardcoded Some())
  private[converter] def calculatePathForVideoAdTag(rawChannel: RawChannel): String =
    inheritanceCalculator.forChannel[String](rawChannel, pathForAdTagAction, c ⇒ c.config.commercial.definesVideoAdTag)

  //todo: return Option[] (no hardcoded Some())
  private[converter] def calculatePathForAdTag(rawChannel: RawChannel): String =
    inheritanceCalculator.forChannel[String](rawChannel, pathForAdTagAction, c ⇒ c.config.commercial.definesAdTag)

  /**
    * Primarily converts the RawChannelHeader from the current RawChannel if its RawChannelHeader is nonEmpty
    * Alternative is to search for a parent that is a `Master` and use its Header (empty headers may be inherited to children)
    **/
  private[converter] def calculateHeader(rawChannel: RawChannel): Option[ApiHeaderConfiguration] =
    rawChannel.config.header.find(_.nonEmpty) // is a valid header defined?
      .orElse(calculateMasterChannel(rawChannel).flatMap(_.config.header)) // or else find a master to inherit its header
      .map { result ⇒
      ApiHeaderConfiguration(
        label = result.label,
        logo = result.logo,
        headerReference = result.headerReference.map(ref ⇒ ApiReference(ref.label, ref.path)),
        slogan = result.slogan,
        sloganReference = result.sloganReference.map(ref ⇒ ApiReference(ref.label, ref.path)),
        sectionReferences = result.sectionReferences.map(refs ⇒ refs.map(ref ⇒ ApiReference(ref.label, ref.path))),
        hidden = Option(result.hidden)
      )
    }

  /**
    * Primarily converts the RawChannelSiteBuilding from the current RawChannel if RawChannelSitebuilding is defined
    * Alternative is to search for a parent that is a `Master` and use its Sitebuilding config (empty sitebuilding may be inherited to children)
    **/
  private[converter] def calculateSiteBuilding(rawChannel: RawChannel): Option[ApiSiteBuildingConfiguration] =
    rawChannel.config.siteBuilding.find(_.isEmpty == false)
      .orElse(calculateMasterChannel(rawChannel).flatMap(_.config.siteBuilding)) // or else find a master to inherit its header
      .map { result ⇒
      ApiSiteBuildingConfiguration(
        fields = result.fields,
        sub_navigation = result.sub_navigation.map(refs ⇒
          refs.map(ref ⇒ ApiReference(ref.label, ref.path))
        ),
        elements = result.elements.map(
          refs ⇒ refs.map(ref ⇒
            ApiElement(
              id = ref.id,
              `type` = ref.`type`,
              assets = ref.assets.map(refs2 ⇒ refs2.map(ref2 ⇒ ApiAsset(`type` = ref2.`type`, fields = ref2.fields)))
            )
          )
        )
      )
    }

  /**
    * Link to Master Channel in the top of a section page or an article page
    * e.g. there is a link to /icon/ with Label Icon on /icon/uhren/
    */
  private[converter] def calculateMasterReference(rawChannel: RawChannel): Option[ApiReference] = {
    calculateMasterChannel(rawChannel).flatMap { master ⇒
      Some(ApiReference(label = Some(master.id.label), href = Some(master.id.path)))
    }
  }

  private[converter] def trimPathForAdTag(path: String): String = {
    val pathWithoutLeadingAndTrailingSlashes = path.stripPrefix("/").stripSuffix("/").trim
    Option(pathWithoutLeadingAndTrailingSlashes).filter(_.nonEmpty).getOrElse("sonstiges")
  }

  /**
    * Calculate the Master for any given Channel
    * Channels are a `Master` if
    * a) the `Master` Option under GodMode in CMCF is checked
    * b) the channel is 1st level (== parent is frontpage)
    * Master Channel may inherit some of its properties
    * e.g. on /sport/fussball/1.bundesliga/ the Header shows "Fußball" linked to /sport/fussball/ to allow user to click upwards in the tree
    * e.g. on /sport/fussball/1.bundesliga/ the same SubNavigation is shown as on /sport/fussball/ to allow users to click through the page
    * but ConfigMcConfigFace Users only need to define the Subnavigation once
    */
  private[converter] def calculateMasterChannel(rawChannel: RawChannel): Option[RawChannel] = {
    val masterChannelInheritanceAction: InheritanceAction[Option[RawChannel]] = InheritanceAction[Option[RawChannel]](
      forRoot = ch ⇒ Some(ch.root),
      forFallback = _ ⇒ None,
      forMatching = masterChannel ⇒ Some(masterChannel)
    )
    val predicate: RawChannel ⇒ Boolean = c ⇒ c.parent.contains(c.root) || c.config.master
    inheritanceCalculator.forChannel[Option[RawChannel]](rawChannel, masterChannelInheritanceAction, predicate)
  }


  private[converter] def calculateBrand(rawChannel: RawChannel): Boolean = {
    val brandInheritanceAction: InheritanceAction[Boolean] = InheritanceAction[Boolean](
      forRoot = _ ⇒ false, // root is never a brand
      forFallback = _ ⇒ false, // last channel before root with brand == false
      forMatching = _ ⇒ true // ignore `c` -- it's always `true`
    )
    inheritanceCalculator.forChannel[Boolean](rawChannel, brandInheritanceAction, c ⇒ c.config.brand)
  }

  private[converter] def calculateTheme(rawChannel: RawChannel): Option[ApiThemeConfiguration] = {
    val maybeThemeMapping: RawChannel ⇒ Option[ApiThemeConfiguration] = c ⇒ c.config.theme.map(t ⇒ ApiThemeConfiguration(t.name, t.fields))
    val themeInheritanceAction: InheritanceAction[Option[ApiThemeConfiguration]] = InheritanceAction[Option[ApiThemeConfiguration]](
      forRoot = _ ⇒ None, // The Frontpage has never a theme
      forFallback = maybeThemeMapping,
      forMatching = maybeThemeMapping
    )
    inheritanceCalculator.forChannel[Option[ApiThemeConfiguration]](rawChannel, themeInheritanceAction, c ⇒ c.config.theme.exists(_.name.isDefined))
  }

  private[converter] def apiMetaConfigurationFromRawChannel(rawChannel: RawChannel): Option[ApiMetaConfiguration] = {
    rawChannel.config.metadata.map(metadata => ApiMetaConfiguration(
      title = metadata.title.filter(_.nonEmpty),
      description = metadata.description.filter(_.nonEmpty),
      tags = metadata.keywords,
      contentMetaRobots = metadata.contentRobots.map(apiMetaRobotsFromRawChannelMetaRobotsTag),
      sectionMetaRobots = metadata.sectionRobots.map(apiMetaRobotsFromRawChannelMetaRobotsTag)
    ))
  }

  private[converter] def apiMetaRobotsFromRawChannelMetaRobotsTag(rawChannelMetaRobotsTag: RawChannelMetaRobotsTag): ApiMetaRobots =
    ApiMetaRobots(noIndex = rawChannelMetaRobotsTag.noIndex, noFollow = rawChannelMetaRobotsTag.noFollow)

  /**
    * Always calculates adTags, in doubt 'sonstiges' -> not optional
    *
    * @param rawChannel source Channel to take the data from
    * @return a resolved 'ApiCommercialConfiguration' containing a videoAdTag and an adTag
    */
  private[converter] def apiCommercialConfigurationFromRawChannel(rawChannel: RawChannel): ApiCommercialConfiguration = {
    ApiCommercialConfiguration(
      pathForAdTag = Some(calculatePathForAdTag(rawChannel)),
      pathForVideoAdTag = Some(calculatePathForVideoAdTag(rawChannel)),
      thirdParty = Some(thirdPartyCommercialFromRawChannelCommercial(rawChannel.config.commercial)),
      adIndicator = rawChannel.config.header.map(_.adIndicator),
      showFallbackAds = Some(rawChannel.config.commercial.showFallbackAds),
      disableAdvertisement = Some(rawChannel.config.commercial.disableAdvertisement)
    )
  }

  private[converter] def apiSponsoringConfigurationFromRawChannel(rawChannel: RawChannel): ApiSponsoringConfiguration = {
    ApiSponsoringConfiguration(
      name = rawChannel.config.sponsoring.logo,
      logo = rawChannel.config.sponsoring.logo,
      slogan = rawChannel.config.sponsoring.slogan,
      hidden = Some(rawChannel.config.sponsoring.hidden),
      link = rawChannel.config.sponsoring.link.map(ref ⇒ ApiReference(ref.label, ref.path)),
      brandstation = rawChannel.config.sponsoring.brandstation
    )
  }

  private[converter] def apiThemeFromRawChannel(rawChannel: RawChannel): Option[ApiThemeConfiguration] =
    rawChannel.config.theme.map(t ⇒ ApiThemeConfiguration(t.name, t.fields))

  //todo: return Option[ApiCommercial3rdPartyConfiguration] (no hardcoded Some())
  private[converter] def thirdPartyCommercialFromRawChannelCommercial(rawChannelCommercial: RawChannelCommercial) =
    ApiCommercial3rdPartyConfiguration(
      contentTaboola = Some(ApiCommercialTaboolaConfiguration(
        showNews = Some(rawChannelCommercial.contentTaboola.showNews),
        showWeb = Some(rawChannelCommercial.contentTaboola.showWeb),
        showWebExtended = Some(rawChannelCommercial.contentTaboola.showWebExtended),
        showNetwork = Some(rawChannelCommercial.contentTaboola.showNetwork)
      ))
    )
}
