package de.welt.contentapi.menu.services

import com.google.inject.ImplementedBy
import de.welt.contentapi.core.client.services.s3.S3Client
import de.welt.contentapi.menu.models.{Menu, MenuMetadata}
import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.{Configuration, Environment, Logger}

@ImplementedBy(classOf[AdminMenuServiceImpl])
trait AdminMenuService {
  def get(): Option[Menu]
  def save(menuData: Menu, user: String): Menu
}

@Singleton
class AdminMenuServiceImpl @Inject()(config: Configuration,
                                     environment: Environment,
                                     s3: S3Client) extends AdminMenuService with MenuServiceUtils {

  import de.welt.contentapi.menu.models.MenuFormats._

  private[services] val s3Config: S3Config = s3Config(config, environment.mode)

  override def get(): Option[Menu] = loadMenu(s3, s3Config)

  override def save(menuData: Menu, user: String): Menu = {
    val menuDataWithUpdatedMetadata: Menu = menuData.copy(metadata = MenuMetadata(changedBy = user))
    val serializedMenuData = Json.toJson(menuDataWithUpdatedMetadata).toString()
    val filePath: String = s3FilePath(s3Config)

    Logger.info(s"saving menu data to s3 -> $s3Config.bucket/$filePath - user: $user")

    s3.putPrivate(s3Config.bucket, filePath, serializedMenuData, "application/json")

    menuDataWithUpdatedMetadata
  }
}
