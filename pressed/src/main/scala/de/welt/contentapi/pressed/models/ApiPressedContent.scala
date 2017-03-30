package de.welt.contentapi.pressed.models

import de.welt.contentapi.core.models.ApiContent

/**
  * @param content       A single content item. (Frank)
  * @param related       Related content of the main content -- article and playlist. (Frank)
  * @param channel       Channel with breadcrumb of the content. (ConfigMcConfigFace)
  * @param configuration Configuration for the content page. (ConfigMcConfigFace)
  */
case class ApiPressedContent(content: ApiContent,
                             related: Option[Seq[ApiPressedContent]] = None,
                             channel: Option[ApiChannel] = None,
                             configuration: Option[ApiConfiguration] = None) {
  private lazy val unwrappedRelated: Seq[ApiPressedContent] = related.getOrElse(Nil)
  lazy val relatedContent: Seq[ApiPressedContent] = relatedFilteredBy("related")
  lazy val relatedMoreLikeThis: Seq[ApiPressedContent] = relatedFilteredBy("more-like-this")
  lazy val relatedPlaylist: Seq[ApiPressedContent] = relatedFilteredBy("playlist")

  private[this] def relatedFilteredBy(`type`: String): Seq[ApiPressedContent] = unwrappedRelated.filter(_.content.unwrappedRoles.contains(`type`))
}

