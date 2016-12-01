package de.welt.contentapi.pressed.models

import de.welt.contentapi.core.models.ApiReference

/**
  * ApiChannel is a simple frontend view on 'section data' with the section itself and its parents
  *
  * @param section the current section itself as ApiSectionReference
  * @param breadcrumb the current section and all parents until the 'frontpage' aka '/' aka 'home' as a Seq of ApiSectionReferences
  */
case class ApiChannel(section: Option[ApiReference] = None, breadcrumb: Option[Seq[ApiReference]] = None) {
  lazy val unwrappedBreadcrumb: Seq[ApiReference] = breadcrumb.getOrElse(Nil)
}
