package de.welt.contentapi.pressed.models

import java.time.Instant

import de.welt.contentapi.pressed.models.StatusPhrase.StatusPhrase
import play.api.http.Status

/**
  * A wrapper around a [[ApiPressedSection]] that adds additional information
  *
  * @param source      the source where this response originated (e.g. digger/s3)
  * @param section     optional section response, may be empty in case of errors
  * @param status      the HTTP status text for this response
  * @param statusCode  the HTTP status code for this response
  * @param createdDate when was this response created
  */
case class ApiPressedSectionResponse(source: String,
                                     section: Option[ApiPressedSection],
                                     status: StatusPhrase = StatusPhrase.ok,
                                     statusCode: Int = Status.OK,
                                     createdDate: Instant = Instant.now)

object StatusPhrase extends Enumeration {
  type StatusPhrase = Value
  val ok, no_content, section_not_found, internal_error = Value
}

/**
  * A complete section page with all teasers and page configuration.
  *
  * @param stages        all stages of the section. Includes (pressed) content teasers and the config.
  * @param channel       channel with breadcrumb of the section. (ConfigMcConfigFace)
  * @param configuration configuration for the section page. (ConfigMcConfigFace)
  */
case class ApiPressedSection(stages: Seq[ApiStage] = Nil,
                             channel: Option[ApiChannel] = None,
                             configuration: Option[ApiConfiguration] = None)