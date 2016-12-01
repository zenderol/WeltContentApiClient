package de.welt.contentapi.core.models

/**
  * @param content single content item
  * @param related related stuff like content or playlist of the single content item
  */
case class ApiResponse(content: ApiContent,
                       related: Option[List[ApiContent]] = None) {
  lazy val unwrappedRelated: List[ApiContent] = related.getOrElse(Nil)
  lazy val relatedContent: List[ApiContent] = relatedFilteredBy("related")
  lazy val relatedPlaylist: List[ApiContent] = relatedFilteredBy("playlist")

  private[this] def relatedFilteredBy(`type`: String): List[ApiContent] = unwrappedRelated.filter(_.unwrappedRoles.contains(`type`))
}

/**
  * @param webUrl   relative url of the content
  * @param `type`   main content type (Escenic based) like: article, video, live
  * @param id       Escenic ID. This is needed for: tracking, commercials, videos
  * @param subType  e.g. satire, commentary
  * @param fields   generic map with 'data/content' based on the content type
  * @param authors  authors of the content
  * @param elements all elements for the content. A element has a relation (teaser, opener, inline) and type (image, video)
  * @param roles    needed for related content (role playlist) (Frank)
  * @param sections ???
  * @param tags     Tags of the content. Only the Escenic tags (manuel created).
  * @param onward   non resolved relations (related content) (GraphQl)
  */
case class ApiContent(webUrl: String,
                      `type`: String,
                      id: Option[String] = None,
                      subType: Option[String] = None,
                      fields: Option[Map[String, String]] = None,
                      authors: Option[List[ApiAuthor]] = None,
                      elements: Option[List[ApiElement]] = None,
                      roles: Option[List[String]] = None,
                      sections: Option[ApiSectionData] = None,
                      tags: Option[List[ApiTag]] = None,
                      onward: Option[List[ApiOnward]] = None) {

  lazy val unwrappedFields: Map[String, String] = fields.getOrElse(Map.empty[String, String])
  lazy val unwrappedAuthors: List[ApiAuthor] = authors.getOrElse(Nil)
  lazy val unwrappedElements: List[ApiElement] = elements.getOrElse(Nil)
  lazy val unwrappedRoles: List[String] = roles.getOrElse(Nil)
  lazy val unwrappedTags: List[ApiTag] = tags.getOrElse(Nil)

  def fieldsContainEntry(entry: (String, String)): Boolean = unwrappedFields.exists(_ == entry)

  def getMandatoryFieldEntry(key: String): String = unwrappedFields
    .getOrElse(key, throw new IllegalStateException(s"Mandatory field '$key' not found. Content is not valid. Check BACKEND."))
}

/**
  * TODO MANA
  *
  * @param id    ???
  * @param roles ???
  */
case class ApiOnward(id: String, roles: Seq[String])

/**
  * @param id       escenic id of author content page (author page)
  * @param name     name of the author
  * @param position position of the author
  * @param url      relative web url the author page
  * @param elements images of the author
  */
case class ApiAuthor(id: Option[String] = None,
                     name: Option[String] = None,
                     position: Option[String] = None,
                     url: Option[String] = None,
                     elements: Option[List[ApiElement]] = None) {
  lazy val unwrappedElements: List[ApiElement] = elements.getOrElse(Nil)
}

/**
  * @param id        unique identifier from WeltN24/brian to find a single element inside the content body text
  * @param `type`    type of element like: image, video, oembed ...
  * @param relations on witch relations is the element used: Teaser, Opener, Closer, Inline ...
  * @param assets    all assets of the element: image asset with url, video asset with url, poster, width, heidht
  */
case class ApiElement(id: String,
                      `type`: String,
                      assets: Option[List[ApiAsset]],
                      relations: Option[List[String]] = None) {
  lazy val unwrappedRelations: List[String] = relations.getOrElse(Nil)
  lazy val unwrappedAssets: List[ApiAsset] = assets.getOrElse(Nil)
  lazy val metadataAsset: Option[ApiAsset] = unwrappedAssets.find(_.`type` == "metadata")
}

/**
  * WTF? Irgendwas mit VIDEO
  *
  * @param validToDate ???
  */
case class ApiMetadata(validToDate: String) {
  def asMap: Map[String, String] = Map(
    "validToDate" -> validToDate
  )
}

/**
  * @param `type`   type of the asset like: image, video
  * @param fields   generic 'data/content' based on the type of asset. E.g. source, width, height
  * @param metadata api processing meta data like: state, fetchErrors, transformationDate
  * @param index    the index for multiple assets like a gallery
  */
case class ApiAsset(`type`: String,
                    fields: Option[Map[String, String]] = None,
                    metadata: Option[ApiMetadata] = None,
                    index: Option[Int] = None) {
  lazy val unwrappedFields: Map[String, String] = fields.getOrElse(Map.empty[String, String])
  lazy val unwrappedMetadata: Map[String, String] = metadata.map(_.asMap).getOrElse(Map.empty[String, String])
}

/**
  *
  * @param home ???
  * @param all  ???
  */
case class ApiSectionData(home: Option[String], all: Option[List[String]] = None)

/**
  * @param id    the real value of the tag
  * @param value crap escenic format. don't use this.
  */
case class ApiTag(id: Option[String], value: Option[String] = None)

/**
  * A reference <a/> to a section.
  *
  * Naming-Refactoring: Maybe is ApiReference better? Think of internal and external links. All internal links are
  * relative and all external absolute. And what about links to content pages?
  *
  * @param label label of the <a/>
  * @param href  href of the <a/>
  */
case class ApiReference(label: Option[String] = None, href: Option[String] = None)
