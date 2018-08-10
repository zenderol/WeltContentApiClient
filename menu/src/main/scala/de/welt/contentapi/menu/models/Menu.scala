package de.welt.contentapi.menu.models

import java.time.Instant

/**
  * The menu at welt.de consists of two independent parts (left and right).
  * In order to distinguish between them we need two separate lists: Primary and Secondary.
  *
  * @param primaryMenu   Left side of the menu (e.g. Home, Sport, Politik etc.).
  * @param secondaryMenu Right side of the menu (e.g. Produkte).
  * @param metadata      Meta data for CMCF. Needed for some merge/update/locking logic.
  */
case class Menu(primaryMenu: Seq[MenuLink] = Seq.empty,
                secondaryMenu: Seq[MenuLink] = Seq.empty,
                var metadata: MenuMetadata = MenuMetadata())

/**
  * Metadata for stored menu data.
  *
  * @param changedBy        github id of last sitebuilder
  * @param lastModifiedDate timestamp of last change
  */
case class MenuMetadata(changedBy: String = "system",
                        lastModifiedDate: Long = Instant.now.toEpochMilli)

/**
  * A menu link is basically a link with special properties and possible children.
  *
  * @param link       The actual link.
  * @param commercial Is the item used for commercial purposes?
  * @param children   Holds all children of this link.
  */
case class MenuLink(link: MenuReference,
                    commercial: Boolean = false,
                    children: Seq[MenuLink] = Seq.empty)

/**
  * A menu reference is model to render a link within the menu.
  *
  * @param label label of the link.
  * @param path  path of the link.
  */
case class MenuReference(label: Option[String] = None, path: Option[String] = None)
