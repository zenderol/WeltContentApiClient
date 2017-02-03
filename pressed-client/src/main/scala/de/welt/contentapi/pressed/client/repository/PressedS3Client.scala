package de.welt.contentapi.pressed.client.repository

import java.time.Instant
import javax.inject.Inject

import de.welt.contentapi.core.client.services.s3.S3Client
import de.welt.contentapi.pressed.models.ApiPressedSectionResponse
import de.welt.contentapi.pressed.models.PressedReads.apiPressedSectionResponseReads
import de.welt.contentapi.utils.Loggable
import play.api.Configuration
import play.api.libs.json.{JsError, JsSuccess, Json}

sealed trait PressedS3Client {
  /**
    * find a pre-pressed section on S3
    *
    * @param path the section's path
    *
    * @return a tuple of a [[ApiPressedSectionResponse]] and its last mode time (of instance [[Instant]] <br/>
    *         None if section was not found in S3
    *
    */
  def find(path: String): Option[(ApiPressedSectionResponse, Instant)]
}

case class PressedS3ClientImpl @Inject()(s3Client: S3Client, config: Configuration) extends PressedS3Client with Loggable {

  import PressedS3ClientImpl._

  protected val bucket: String = config.getString(bucketConfigKey)
    .getOrElse(throw config.reportError(bucketConfigKey, s"Missing Configuration value: $bucketConfigKey"))
  private val file: String = config.getString(fileConfigKey)
    .getOrElse(throw config.reportError(fileConfigKey, s"Missing Configuration value: $fileConfigKey"))

  protected def getKeyForPath(path: String) = file + path + "pressed.json"

  override def find(path: String): Option[(ApiPressedSectionResponse, Instant)] = {

    val key = getKeyForPath(path)

    s3Client.getWithLastModified(bucket, key).flatMap {

      case (json, lastMod) ⇒

        Json.parse(json).validate[ApiPressedSectionResponse](apiPressedSectionResponseReads) match {
          case JsSuccess(value, _) ⇒
            Some(value, lastMod)
          case err@JsError(_) ⇒
            log.warn(s"Unable to parse content at (bucket: '$bucket' key: '$key'). Reason: '${err.toString}'")
            None
        }
    }
  }
}

object PressedS3ClientImpl {
  val bucketConfigKey = "welt.aws.s3.pressed.bucket"
  val fileConfigKey = "welt.aws.s3.pressed.file"
}
