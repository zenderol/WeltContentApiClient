package de.welt.contentapi.pressed.models

import de.welt.contentapi.core.models.ApiReference

/**
  * [[ApiChannel]] is a simple frontend view on 'section data' with the section itself and its parents
  *
  * @param section    the current section itself as [[ApiReference]]
  * @param breadcrumb the current section and all parents until the 'frontpage' aka '/' aka 'home'
  *                   as a Seq[[ApiReference]].
  * @param brand      flags this channel as a 'brand'. A brand is a "Sub-Marke" like Icon ('/icon/') with
  *                   different UI elements or layouts.
  */
case class ApiChannel(section: Option[ApiReference] = None,
                      breadcrumb: Option[Seq[ApiReference]] = None,
                      brand: Option[Boolean] = None) {
  lazy val unwrappedBreadcrumb: Seq[ApiReference] = breadcrumb.getOrElse(Nil)
  lazy val isBrand: Boolean = brand.getOrElse(false)
}
