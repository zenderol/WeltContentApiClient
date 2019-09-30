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
                             configuration: Option[ApiConfiguration] = None,
                             var embeds: Option[Seq[ApiPressedEmbed]] = None) {

  import de.welt.contentapi.pressed.models.ApiPressedContentRoles._

  private lazy val unwrappedRelated: Seq[ApiPressedContent] = related.getOrElse(Nil)
  lazy val relatedContent: Seq[ApiPressedContent] = relatedFilteredBy(Related)
  lazy val relatedMoreLikeThis: Seq[ApiPressedContent] = relatedFilteredBy(MLT)
  lazy val relatedPlaylist: Seq[ApiPressedContent] = relatedFilteredBy(Playlist)
  lazy val relatedAuthorContent: Seq[ApiPressedContent] = relatedFilteredBy(Author)
  lazy val relatedMoreFromAuthor: Seq[ApiPressedContent] = relatedFilteredBy(MoreFromAuthor)
  lazy val relatedPromotions: Seq[ApiPressedContent] = relatedFilteredBy(Promotion)
  lazy val unwrappedEmbeds: Seq[ApiPressedEmbed] = embeds.getOrElse(Nil)

  private[this] def relatedFilteredBy(role: Role): Seq[ApiPressedContent] = unwrappedRelated.filter(_.content.unwrappedRoles.contains(role.name))

  def relatedByRole(role: ApiPressedContentRoles.Role*): Seq[ApiPressedContent] =
    unwrappedRelated.filter(_.content.unwrappedRoles.intersect(role.map(_.name)).nonEmpty)
}

object ApiPressedContentRoles {

  // @formatter:off
  sealed trait Role { def name: String }
  case object Related extends Role { val name = "related" }
  case object MLT extends Role { val name = "more-like-this" }
  case object Playlist extends Role { val name = "playlist" }
  case object MoreFromAuthor extends Role { val name = "more-from-author" }
  case object Author extends Role { val name = "author" }
  case object Promotion extends Role { val name = "promotion" }
  // @formatter:on

}
