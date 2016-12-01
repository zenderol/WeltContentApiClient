package de.welt.contentapi.pressed.models

import de.welt.contentapi.core.models.ApiReference

/**
  * Configuration for a content or section page. All configs are optional.
  * This means that they can be overwritten (by ConfigMcConfigFace) but not required. All clients must define some kind
  * of fallback or default values.
  *
  * @param meta       configuration for <meta> tag overrides
  * @param commercial commercial configuration
  * @param sponsoring branding is part of the page header (page sponsoring). E.g. Formel1
  * @param header     (content) page header configuration. Not the real page header.
  * @param theme      theme of the page
  */
case class ApiConfiguration(meta: Option[ApiMetaConfiguration] = None,
                            commercial: Option[ApiCommercialConfiguration] = None,
                            sponsoring: Option[ApiSponsoringConfiguration] = None,
                            header: Option[ApiHeaderConfiguration] = None,
                            theme: Option[ApiThemeConfiguration] = None)

/**
  * <meta> configuration for content or section pages
  *
  * @param title             <title> override
  * @param description       <meta> description override
  * @param tags              <meta> keyword override
  * @param contentMetaRobots override `<meta name="robots">` tag only for all content pages of the channel.
  * @param sectionMetaRobots override `<meta name="robots">` tag only for the section page of the channel.
  */
case class ApiMetaConfiguration(title: Option[String] = None,
                                description: Option[String] = None,
                                tags: Option[Seq[String]] = None,
                                contentMetaRobots: Option[ApiMetaRobots] = None,
                                sectionMetaRobots: Option[ApiMetaRobots] = None) {
  lazy val unwrappedTags: Seq[String] = tags.getOrElse(Nil)
}

/**
  * <meta name="robots" content="index,follow,noodp">
  *
  * @param noIndex  `true` == 'noIndex' & `false` == 'index'
  * @param noFollow `true` == 'noFollow' & `false` == 'follow'
  */
case class ApiMetaRobots(noIndex: Option[Boolean] = None, noFollow: Option[Boolean] = None)

/**
  * Some overrides for commercial settings (ASMI). Per default all commercial configuration based on the section path.
  *
  * @param pathForAdTag      path used to build the ad tag in client
  * @param pathForVideoAdTag path used to build the video ad tag in client
  */
case class ApiCommercialConfiguration(pathForAdTag: Option[String] = None, pathForVideoAdTag: Option[String] = None)

/**
  * Branding or sponsoring of section and content pages. This is only the name of the branding. The impl is part of
  * the client.
  *
  * @param name name of the branding. Need for mapping.
  */
case class ApiSponsoringConfiguration(name: Option[String] = None)

/**
  * Some configuration for the section or content page header. Not the real page header.
  *
  * @param label             label for the section/content page. Used by escenic section title but can be overridden by janus/cmcf
  * @param logo              mapping name for the client. When a logo is configured by janus/cmcf it overrides the label
  * @param slogan            optional slogan for the label/logo
  * @param sectionReferences section refs for linking
  */
case class ApiHeaderConfiguration(label: Option[String] = None,
                                  logo: Option[String] = None,
                                  slogan: Option[String] = None,
                                  sectionReferences: Option[Seq[ApiReference]] = None) {
  lazy val unwrappedSectionReferences: Seq[ApiReference] = sectionReferences.getOrElse(Nil)
}

/**
  * Theme of the section or content page. Mostly for some background color changes e.g. mediathek. This is only
  * the name of the theme. The impl is part of the client.
  *
  * @param name   name of the theme. Need for mapping.
  * @param fields optional settings/hints/configuration of the theme
  */
case class ApiThemeConfiguration(name: Option[String] = None, fields: Option[Map[String, String]] = None) {
  lazy val unwrappedFields: Map[String, String] = fields.getOrElse(Map.empty[String, String])
}
