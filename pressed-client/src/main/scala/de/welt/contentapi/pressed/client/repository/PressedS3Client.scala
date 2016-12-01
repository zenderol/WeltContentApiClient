package de.welt.contentapi.pressed.client.repository

import java.time.Instant
import javax.inject.Inject

import de.welt.contentapi.core.client.services.s3.S3Client
import de.welt.contentapi.pressed.models.{ApiPressedSection, PressedReads}
import de.welt.contentapi.utils.Loggable
import play.api.Configuration
import play.api.libs.json.{JsError, JsSuccess, Json}

sealed trait PressedS3Client {
  def find(path: String): Option[(ApiPressedSection, Instant)]
}


case class PressedS3ClientImpl @Inject()(s3Client: S3Client, config: Configuration) extends PressedS3Client with Loggable {

  import PressedS3ClientImpl._
  val bucket = config.getString(bucketConfigKey).getOrElse(throw config.reportError(bucketConfigKey, "Missing Configuration value"))
  val file = config.getString(fileConfigKey).getOrElse(throw config.reportError(fileConfigKey, "Missing Configuration value"))

  override def find(path: String): Option[(ApiPressedSection, Instant)] = {

    s3Client.getWithLastModified(bucket, path + file).flatMap {

      case (json, lastMod) ⇒

        Json.parse(json).validate[ApiPressedSection](PressedReads.apiPressedSectionReads) match {
          case JsSuccess(value, _) ⇒
            Some(value, lastMod)
          case err@JsError(_) ⇒
            log.warn(s"Unable to parse content at '$bucket$path$file'. Reason: '${err.toString}'")
            None
        }
    }
  }
}

object PressedS3ClientImpl {
  val bucketConfigKey = "welt.aws.s3.pressed.bucket"
  val fileConfigKey = "welt.aws.s3.pressed.file"
}