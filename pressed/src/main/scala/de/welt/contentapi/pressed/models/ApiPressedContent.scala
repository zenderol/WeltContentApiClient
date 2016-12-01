package de.welt.contentapi.pressed.models

import de.welt.contentapi.core.models.ApiContent

/**
  * @param content       a single content. (Frank)
  * @param related       related teaser. (Frank)
  * @param channel       channel with breadcrumb of the content. (ConfigMcConfigFace)
  * @param configuration configuration for the content page. (ConfigMcConfigFace)
  */
case class ApiPressedContent(content: ApiContent,
                             related: Option[Seq[ApiPressedContent]] = None,
                             channel: Option[ApiChannel] = None,
                             configuration: Option[ApiConfiguration] = None) {
  private lazy val unwrappedRelated: Seq[ApiPressedContent] = related.getOrElse(Nil)
  lazy val relatedContent: Seq[ApiPressedContent] = relatedFilteredBy("related")
  lazy val relatedPlaylist: Seq[ApiPressedContent] = relatedFilteredBy("playlist")

  private[this] def relatedFilteredBy(`type`: String): Seq[ApiPressedContent] = unwrappedRelated.filter(_.content.unwrappedRoles.contains(`type`))
}

