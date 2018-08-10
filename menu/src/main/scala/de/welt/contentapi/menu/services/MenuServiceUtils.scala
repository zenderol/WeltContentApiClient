package de.welt.contentapi.menu.services

import de.welt.contentapi.core.client.services.s3.S3Client
import de.welt.contentapi.menu.models.Menu
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.{Configuration, Logger, Mode}

case class S3Config(bucket: String, folder: String, file: String, environment: String)

object MenuConfig {
  val bucketConfigKey = "welt.aws.s3.menuData.bucket"
  val folderConfigKey = "welt.aws.s3.menuData.folder"
  val fileConfigKey = "welt.aws.s3.menuData.file"
}

trait MenuServiceUtils {

  def s3Config(config: Configuration, mode: Mode): S3Config = {
    val bucket: String = config.get[String](MenuConfig.bucketConfigKey)
    val folder: String = config.get[String](MenuConfig.folderConfigKey)
    val file: String = config.get[String](MenuConfig.fileConfigKey)
    val environment: String = mode match {
      case Mode.Prod ⇒ "prod"
      case _ ⇒ "dev"
    }

    S3Config(bucket, folder, file, environment)
  }

  def s3FilePath(s3Config: S3Config): String = s"${s3Config.folder}/${s3Config.environment}/${s3Config.file}"

  def loadMenu(s3Client: S3Client, s3Config: S3Config): Option[Menu] = {
    import de.welt.contentapi.menu.models.MenuFormats._

    val s3Content: Option[String] = s3Client.get(s3Config.bucket, s3FilePath(s3Config))

    val maybeMenu: Option[Menu] = s3Content.flatMap { s3Content: String ⇒
      val validationResult = Json.parse(s3Content)

      validationResult.validate[Menu] match {
        case JsSuccess(menu, _) ⇒
          Logger.debug("S3 Menu Data successfully loaded.")
          Some(menu)
        case err@JsError(_) ⇒
          Logger.error("S3 Menu Data could not be parsed.", new scala.IllegalArgumentException(err.errors.head.toString))
          None
      }
    }

    maybeMenu
  }
}
