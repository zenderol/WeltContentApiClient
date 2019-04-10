package de.welt.contentapi.pressed.client.repository

import java.time.Instant

import com.google.inject.{ImplementedBy, Inject, Singleton}
import de.welt.contentapi.core.client.services.aws.s3.S3Client
import de.welt.contentapi.core.client.services.configuration.ApiConfiguration
import de.welt.contentapi.pressed.models.ApiPressedSectionResponse
import de.welt.contentapi.pressed.models.PressedReads.apiPressedSectionResponseReads
import de.welt.contentapi.utils.Loggable
import play.api.libs.json.{JsError, JsSuccess, Json}

@ImplementedBy(classOf[PressedS3ClientImpl])
sealed trait PressedS3Client {
  /**
    * find a pre-pressed section on S3
    *
    * @param path the section's path
    * @return a tuple of a [[ApiPressedSectionResponse]] and its last mode time (of instance [[Instant]] <br/>
    *         None if section was not found in S3
    *
    */
  def find(path: String): Option[(ApiPressedSectionResponse, Instant)]
}

@Singleton
class PressedS3ClientImpl @Inject()(s3Client: S3Client) extends PressedS3Client with Loggable {

  private val bucket = ApiConfiguration.aws.s3.raw.file
  private val file = ApiConfiguration.aws.s3.raw.file

  override def find(path: String): Option[(ApiPressedSectionResponse, Instant)] = {

    s3Client.getWithLastModified(bucket, file.replaceFirst("__PATH__", path)).flatMap {

      case (json, lastMod) ⇒
        Json.parse(json).validate[ApiPressedSectionResponse](apiPressedSectionResponseReads) match {
          case JsSuccess(value, _) ⇒
            Some(value, lastMod)
          case err@JsError(_) ⇒
            log.warn(s"Unable to parse content at (bucket: '$bucket' key: '$file'). Reason: '${err.toString}'")
            None
        }
    }
  }
}