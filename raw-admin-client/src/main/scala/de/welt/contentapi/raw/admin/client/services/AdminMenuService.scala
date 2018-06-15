package de.welt.contentapi.raw.admin.client.services

import com.google.inject.ImplementedBy
import de.welt.contentapi.core.client.services.s3.S3Client
import de.welt.contentapi.raw.admin.client.services.AdminMenuServiceImpl.{bucketConfigKey, fileConfigKey, folderConfigKey}
import de.welt.contentapi.raw.models.{RawMenu, RawMetadata}
import de.welt.contentapi.utils.Env.Env
import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.{Configuration, Environment, Logger}

@ImplementedBy(classOf[AdminMenuServiceImpl])
trait AdminMenuService {
  def get()(implicit env: Env): RawMenu
  def save(menuData: RawMenu, user: String)(implicit env: Env): RawMenu
}

@Singleton
class AdminMenuServiceImpl @Inject()(config: Configuration,
                                     s3: S3Client) extends AdminMenuService {

  private[services] val bucket: String = config.get[String](bucketConfigKey)
  private[services] val folder: String = config.get[String](folderConfigKey)
  private[services] val file: String = config.get[String](fileConfigKey)

  private def objectKeyForEnv(env: Env): String = s"$folder/${env.toString}/$file"

  override def get()(implicit env: Env): RawMenu = {
    import de.welt.contentapi.raw.models.RawReads._

    val s3Content: Option[String] = s3.get(bucket, objectKeyForEnv(env))

    s3Content.map { s3Content: String ⇒
      val validationResult = Json.parse(s3Content)

      validationResult.validate[RawMenu] match {
        case JsSuccess(value, _) ⇒
          Logger.debug("S3 Menu Data successfully loaded.")
          value
        case err@JsError(_) ⇒
          Logger.error("S3 Menu Data could not be parsed.", new scala.IllegalArgumentException(err.errors.head.toString))
          RawMenu()
      }
    }.getOrElse(RawMenu())
  }

  override def save(menuData: RawMenu, user: String)(implicit env: Env): RawMenu = {
    import de.welt.contentapi.raw.models.RawWrites._

    val menuDataWithUpdatedMetadata: RawMenu = menuData.copy(metadata = RawMetadata(changedBy = user))
    val json: JsValue = Json.toJson(menuDataWithUpdatedMetadata)
    val serializedMenuData = json.toString
    val environment: String = objectKeyForEnv(env)

    Logger.info(s"saving menu data to s3 -> $bucket/$environment - user: $user")

    s3.putPrivate(bucket, environment, serializedMenuData, "application/json")

    menuDataWithUpdatedMetadata
  }
}

object AdminMenuServiceImpl {
  val bucketConfigKey = "welt.aws.s3.menuData.bucket"
  val folderConfigKey = "welt.aws.s3.menuData.folder"
  val fileConfigKey = "welt.aws.s3.menuData.file"
}
