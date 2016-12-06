package de.welt.contentapi.pressed.client.converter

import de.welt.contentapi.core.models.ApiReference
import de.welt.contentapi.pressed.models._
import de.welt.contentapi.raw.models.{RawChannel, RawChannelCommercial, RawChannelMetaRobotsTag, RawSectionReference}

class RawToApiConverter {

  /** Converter method that takes a rawChannel and returns an ApiChannel from its data
    *
    * @param rawChannel the rawChannel produced by ConfigMcConfigFace
    * @return a new ApiChannel with the data from the rawChannel
    */
  def apiChannelFromRawChannel(rawChannel: RawChannel): ApiChannel = {
    ApiChannel(
      section = Some(getApiSectionReferenceFromRawChannel(rawChannel)),
      breadcrumb = Some(getBreadcrumb(rawChannel)))
  }

  private[converter] def getApiSectionReferenceFromRawChannel(rawChannel: RawChannel): ApiReference = {
    ApiReference(
      label = Some(rawChannel.id.label),
      href = Some(rawChannel.id.path)
    )
  }

  private[converter] def getBreadcrumb(raw: RawChannel): Seq[ApiReference] = raw.getBreadcrumb.map(b⇒ ApiReference(Some(b.id.label), Some(b.id.path)))

  /** Converter method that takes a rawChannel and returns an ApiConfiguration from its data
    *
    * @param rawChannel the rawChannel produced by ConfigMcConfigface
    * @return a new ApiConfiguration Object with the data from the rawChannel
    */
  def apiConfigurationFromRawChannel(rawChannel: RawChannel) = ApiConfiguration(
    meta = apiMetaConfigurationFromRawChannel(rawChannel),
    commercial = Some(apiCommercialConfigurationFromRawChannel(rawChannel)),
    sponsoring = apiSponsoringConfigurationFromRawChannel(rawChannel),
    header = Some(apiHeaderConfigurationFromRawChannel(rawChannel)),
    theme = apiThemeFromRawChannel(rawChannel)
  )

  def calculatePathForVideoAdTag(rawChannel: RawChannel) = calcAdTag(rawChannel, c ⇒ c.definesVideoAdTag)
  def calculatePathForAdTag(rawChannel: RawChannel) = calcAdTag(rawChannel, c ⇒ c.definesAdTag)

  private def calcAdTag(rawChannel: RawChannel, predicate: RawChannelCommercial ⇒ Boolean): String =

    rawChannel.parent match {
      // root
      case None ⇒
        "home"
      // channel is advertised -> calculate Tag
      case Some(parent) if predicate.apply(rawChannel.config.commercial) ⇒ trimPathForAdTag(rawChannel.id.path)
      // is root channel but not advertised, so use fallback
      case Some(parent) if parent == rawChannel.root && !predicate.apply(rawChannel.config.commercial) ⇒
        "sonstiges"
      // channel is not advertised but has parents that may be, so go up in tree
      case Some(parent) ⇒
        calcAdTag(parent, predicate)
    }

  private[converter] def trimPathForAdTag(path: String): String = {
    val pathWithoutLeadingAndTrailingSlashes = path.stripPrefix("/").stripSuffix("/").trim
    Option(pathWithoutLeadingAndTrailingSlashes).filter(_.nonEmpty).getOrElse("sonstiges")
  }

  private[converter] def apiMetaConfigurationFromRawChannel(rawChannel: RawChannel): Option[ApiMetaConfiguration] = {
    rawChannel.config.metadata.map(metadata => ApiMetaConfiguration(
      title = metadata.title,
      description = metadata.description,
      tags = metadata.keywords,
      contentMetaRobots = metadata.contentRobots.map(apiMetaRobotsFromRawChannelMetaRobotsTag),
      sectionMetaRobots = metadata.sectionRobots.map(apiMetaRobotsFromRawChannelMetaRobotsTag)
    )
    )
  }

  private[converter] def apiMetaRobotsFromRawChannelMetaRobotsTag(rawChannelMetaRobotsTag: RawChannelMetaRobotsTag): ApiMetaRobots =
    ApiMetaRobots(noIndex = rawChannelMetaRobotsTag.noIndex, noFollow = rawChannelMetaRobotsTag.noFollow)

  /** Always calculates adTags, in doubt 'sonstiges' -> not optional
    *
    * @param rawChannel source Channel to take the data from
    * @return a resolved 'ApiCommercialConfiguration' containing a videoAdTag and an adTag
    */
  private[converter] def apiCommercialConfigurationFromRawChannel(rawChannel: RawChannel): ApiCommercialConfiguration = {
    ApiCommercialConfiguration(
      pathForAdTag = Some(calculatePathForAdTag(rawChannel)),
      pathForVideoAdTag = Some(calculatePathForVideoAdTag(rawChannel))
    )
  }

  private[converter] def apiSponsoringConfigurationFromRawChannel(rawChannel: RawChannel): Option[ApiSponsoringConfiguration] = {
    rawChannel.config.header.map {
      header => ApiSponsoringConfiguration(header.sponsoring)
    }
  }

  private[converter] def apiHeaderConfigurationFromRawChannel(rawChannel: RawChannel) = {
    val apiSectionReferences: Seq[ApiReference] = mapReferences(
      rawChannel.config.header.map(_.unwrappedSectionReferences).getOrElse(Nil)
    )
    ApiHeaderConfiguration(
      label = rawChannel.config.header.flatMap(_.label),
      logo = rawChannel.config.header.flatMap(_.logo),
      slogan = rawChannel.config.header.flatMap(_.slogan),
      sectionReferences = Some(apiSectionReferences)
    )
  }
  /** Simple Raw Reference -> Api Reference Converter
  */
  def mapReferences(references: Seq[RawSectionReference]): Seq[ApiReference] = {
    references.map(ref ⇒ ApiReference(ref.label, ref.path))
  }

  private[converter] def apiThemeFromRawChannel(rawChannel: RawChannel): Option[ApiThemeConfiguration] =
    rawChannel.config.theme.map(t ⇒ ApiThemeConfiguration(t.name, t.fields))

}
