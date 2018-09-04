package de.welt.contentapi.menu.models

import java.time.Instant

import de.welt.contentapi.core.models.ApiReference

/**
  * The menu at welt.de consists of two independent parts (left and right).
  * In order to distinguish between them we need two separate lists: Primary and Secondary.
  *
  * @param primaryMenu   Left side of the menu (e.g. Home, Sport, Politik etc.).
  * @param secondaryMenu Right side of the menu (e.g. Produkte).
  * @param metadata      Meta data for CMCF. Needed for some merge/update/locking logic.
  */
case class ApiMenu(primaryMenu: Seq[ApiMenuLink] = Seq.empty,
                   secondaryMenu: Seq[ApiMenuLink] = Seq.empty,
                   var metadata: ApiMenuMetadata = ApiMenuMetadata()) {

  def isEmpty: Boolean = this.primaryMenu.isEmpty && this.secondaryMenu.isEmpty
}

/**
  * Metadata for stored menu data.
  *
  * @param changedBy        github id of last site builder
  * @param lastModifiedDate timestamp of last change
  */
case class ApiMenuMetadata(changedBy: String = "system",
                           lastModifiedDate: Long = Instant.now.toEpochMilli)

/**
  * A menu link is basically a link with special properties and possible children.
  *
  * @param reference  The actual reference.
  * @param commercial Is the item used for commercial purposes?
  * @param children   Holds all children of this link.
  */
case class ApiMenuLink(reference: ApiReference,
                       commercial: Boolean = false,
                       children: Seq[ApiMenuLink] = Seq.empty)
