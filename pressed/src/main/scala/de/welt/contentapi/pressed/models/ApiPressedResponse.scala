package de.welt.contentapi.pressed.models

import java.time.Instant

import de.welt.contentapi.pressed.models.StatusPhrase.StatusPhrase

/**
  * @param source      the source where this response originated (e.g. digger/s3)
  * @param status      the HTTP status text for this response
  * @param statusCode  the HTTP status code for this response
  */
abstract class ApiPressedResponse(source: String,
                                  status: StatusPhrase = StatusPhrase.ok,
                                  statusCode: Int = StatusPhrase.HttpStatusOk)

/** Allow responses to be paged (eg author and theme pages, also for section pages) */
trait ApiPaging {
  /** total hit count */
  def total: Option[Int]

  /** total pages count, starting at 1 */
  def pages: Option[Int]

  /** articles per search result page */
  def pageSize: Option[Int]

  /** current page */
  def currentPage: Option[Int]

  /** criteria the result set is sorted by */
  def orderBy: Option[String]
}


/**
  * A wrapper around a [[ApiPressedSection]] that adds additional information
  *
  * @param section optional section response, may be empty in case of errors
  */
case class ApiPressedSectionResponse(section: Option[ApiPressedSection],
                                     source: String,
                                     status: StatusPhrase = StatusPhrase.ok,
                                     statusCode: Int = StatusPhrase.HttpStatusOk,
                                     createdDate: Instant = Instant.now)
  extends ApiPressedResponse(source, status, statusCode)


/**
  * A wrapper around a [[ApiPressedContent]] that adds additional information
  *
  * @param result content response, should always be present
  */
case class ApiPressedContentResponse(result: ApiPressedContent,
                                     source: String,
                                     override val total: Option[Int] = None,
                                     override val pages: Option[Int] = None,
                                     override val pageSize: Option[Int] = None,
                                     override val currentPage: Option[Int] = None,
                                     override val orderBy: Option[String] = None)
  extends ApiPressedResponse(source, StatusPhrase.ok, StatusPhrase.HttpStatusOk) with ApiPaging {

  pageSize.foreach(i ⇒ require(i > 0, "pageSize must be greater than 0"))
  currentPage.foreach(i ⇒ require(i > 0, "currentPage must be greater than 0 (starting at 1)"))
  pages.foreach(i ⇒ require(i >= 0, "pages must be ≥ 0 (starting at 0)"))
  total.foreach(i ⇒ require(i >= 0, "total must be ≥ 0 (starting at 0)"))
}

object StatusPhrase extends Enumeration {
  type StatusPhrase = Value
  val ok, no_content, section_not_found, internal_error = Value
  val HttpStatusOk = 200
}