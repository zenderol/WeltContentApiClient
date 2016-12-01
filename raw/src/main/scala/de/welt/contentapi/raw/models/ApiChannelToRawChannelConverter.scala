package de.welt.contentapi.raw.models

import de.welt.contentapi.raw.models.legacy._

/**
  * This is the converter from the old welt-content-api-client-0.4 to welt-content-api-client-0.5 model.
  * 1. shutdown CMCF
  * 2. convert old-json to new-json (write a simple test)
  * 3. upload new-json to s3
  * 4. boot updated CMCF
  */
object ApiChannelToRawChannelConverter {
  def apply(root: ApiChannel): RawChannel = RawChannel(
    id = RawChannelId(
      path = root.id.path,
      label = root.data.label,
      escenicId = root.id.ece
    ),
    config = config(root.data),
    // Info:
    // All old stage configuration are ignored. Currently not used by any app.
    stages = None,
    metadata = root.metadata.map(metadata).getOrElse(RawMetadata()), // re-create with defaults
    parent = None, // do not write parent to prevent loops while serializing JSON
    children = root.children.map(child ⇒ ApiChannelToRawChannelConverter(child))
  )

  private def config(apiChannelData: ApiChannelData): RawChannelConfiguration = {
    RawChannelConfiguration(
      metadata = apiChannelData.fields.map(channelMetadata),
      header = header(apiChannelData),
      // Info:
      // We set the theme config with the ui.
      // No migration from the old ChannelTheme needed.
      theme = None,
      commercial = commercial(apiChannelData.adData)
    )
  }

  private def channelMetadata(fields: Map[String, String]): RawChannelMetadata = RawChannelMetadata(
    title = fields.get("title"),
    description = fields.get("description"),
    // Info:
    // We clean the keywords from a string to Seq[String]
    keywords = fields.get("keywords").map(_.split(",").filterNot(_.isEmpty)),
    // Info:
    // New field
    sectionBreadcrumbDisabled = None,
    contentRobots = contentMetaRobotsContent(fields),
    sectionRobots = contentMetaRobotsSection(fields)
  )

  private def contentMetaRobotsSection(fields: Map[String, String]): Option[RawChannelMetaRobotsTag] = {
    val metaRobotsTag = RawChannelMetaRobotsTag(
      noIndex = fields.get("metaNoIndex").map(_.toBoolean),
      noFollow = fields.get("metaNoFollow").map(_.toBoolean)
    )
    metaRobotsTag match {
      case RawChannelMetaRobotsTag(None, None) ⇒ None
      case valid@RawChannelMetaRobotsTag(_, _) ⇒ Some(valid)
    }
  }

  private def contentMetaRobotsContent(fields: Map[String, String]): Option[RawChannelMetaRobotsTag] = {
    val metaRobotsTag = RawChannelMetaRobotsTag(
      noIndex = fields.get("metaArticleNoIndex").map(_.toBoolean),
      noFollow = fields.get("metaArticleNoFollow").map(_.toBoolean)
    )
    metaRobotsTag match {
      case RawChannelMetaRobotsTag(None, None) ⇒ None
      case valid@RawChannelMetaRobotsTag(_, _) ⇒ Some(valid)
    }
  }

  private def header(apiChannelData: ApiChannelData): Option[RawChannelHeader] = {
    val header = RawChannelHeader(
      sponsoring = apiChannelData.siteBuilding.map(_.theme).collect { case s if !s.isEmpty ⇒ s },
      label = Option(apiChannelData.label).filter(_.nonEmpty),
      // Info:
      // After the migration we split sponsoring and logo.
      // For now: old data == sponsoring
      logo = None,
      // Info:
      // New field. At the moment the slogan is part of the client (Funkotron)
      slogan = None,
      // Info:
      // New field. This is part for some next steps.
      sectionReferences = None
    )

    header match {
      case RawChannelHeader(None, None, None, None, None) ⇒ None
      case valid@RawChannelHeader(_, _, _, _, _) ⇒ Some(valid)
    }
  }

  private def commercial(apiChannelAdData: ApiChannelAdData): RawChannelCommercial = {
    RawChannelCommercial(
      definesAdTag = apiChannelAdData.definesAdTag,
      definesVideoAdTag = apiChannelAdData.definesVideoAdTag.getOrElse(false)
    )
  }

  private def metadata(apiChannelMetadataNew: ApiChannelMetadataNew): RawMetadata = RawMetadata(
    changedBy = apiChannelMetadataNew.changedBy,
    lastModifiedDate = apiChannelMetadataNew.lastModifiedDate
    // Info:
    // modified & isRessort are new fields
  )

}
