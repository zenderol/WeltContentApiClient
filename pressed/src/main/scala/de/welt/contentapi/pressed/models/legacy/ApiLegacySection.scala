package de.welt.contentapi.pressed.models.legacy

import de.welt.contentapi.core.models.ApiContent

/** Da-hood version of a section
  *
  * @param lists all stages with teasers of a section page
  */
case class ApiLegacySection(lists: Option[List[ApiLegacyStage]] = None) {
  lazy val unwrappedLists: List[ApiLegacyStage] = lists.getOrElse(Nil)
}

/**
  * Da-hood version of a stage
  * @param id mapping identifier of da-hood and frontend
  * @param label optional stage label
  * @param content teasers of a stage
  */
case class ApiLegacyStage(id: String,
                          label: Option[String] = None,
                          content: Option[List[ApiContent]] = None) {
  lazy val unwrappedContent: List[ApiContent] = content.getOrElse(Nil)
}
