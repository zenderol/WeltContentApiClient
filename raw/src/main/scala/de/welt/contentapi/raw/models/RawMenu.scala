package de.welt.contentapi.raw.models

/**
  * The menu at welt.de consists of two independent parts (left and right).
  * In order to distinguish between them we need two separate lists: Primary and Secondary.
  *
  * @param primaryMenu   Left side of the menu (e.g. Home, Sport, Politik etc.).
  * @param secondaryMenu Right side of the menu (e.g. Produkte).
  * @param metadata      Meta data for CMCF. Needed for some merge/update/locking logic.
  */
case class RawMenu(primaryMenu: Seq[RawMenuLink] = Seq.empty,
                   secondaryMenu: Seq[RawMenuLink] = Seq.empty,
                   var metadata: RawMetadata = RawMetadata())

/**
  * A menu link is basically a link with special properties and possible children.
  *
  * @param link       The actual link.
  * @param commercial Is the item used for commercial purposes?
  * @param children   Holds all children of this link.
  */
case class RawMenuLink(link: RawSectionReference,
                       commercial: Boolean = false,
                       children: Seq[RawMenuLink] = Seq.empty)
