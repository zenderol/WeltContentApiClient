package de.welt.contentapi.menu.services

import de.welt.contentapi.core.client.services.s3.S3Client
import de.welt.contentapi.menu.models.ApiMenu
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.{Configuration, Logger, Mode}

case class S3Config(bucket: String, folder: String, file: String, environment: String) {
  val fullFilePath: String = s"$folder/$environment/$file"
}

object MenuConfig {
  val bucketConfigKey = "welt.aws.s3.menuData.bucket"
  val folderConfigKey = "welt.aws.s3.menuData.folder"
  val fileConfigKey = "welt.aws.s3.menuData.file"
}

abstract class MenuServiceRepository(private val config: Configuration, private val mode: Mode) {

  val s3Config: S3Config = {
    val bucket: String = config.get[String](MenuConfig.bucketConfigKey)
    val folder: String = config.get[String](MenuConfig.folderConfigKey)
    val file: String = config.get[String](MenuConfig.fileConfigKey)
    val environment: String = mode match {
      case Mode.Prod ⇒ "prod"
      case _ ⇒ "dev"
    }

    S3Config(bucket, folder, file, environment)
  }

  def loadMenu(s3Client: S3Client): Option[ApiMenu] = {
    import de.welt.contentapi.menu.models.MenuFormats._

    val s3Content: Option[String] = s3Client.get(s3Config.bucket, s3Config.fullFilePath)

    s3Content.flatMap { s3Content: String ⇒
      Json.parse(s3Content).validate[ApiMenu] match {
        case JsSuccess(menu, _) ⇒
          Logger.debug("S3 Menu Data successfully loaded.")
          Some(menu)
        case err@JsError(_) ⇒
          Logger.error("S3 Menu Data could not be parsed.", new scala.IllegalArgumentException(err.errors.head.toString))
          None
      }
    }
  }
}
