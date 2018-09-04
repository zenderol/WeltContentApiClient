package de.welt.contentapi.menu.services

import com.google.inject.ImplementedBy
import de.welt.contentapi.core.client.services.s3.S3Client
import de.welt.contentapi.menu.models.{ApiMenu, ApiMenuMetadata}
import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.{Configuration, Environment, Logger}

@ImplementedBy(classOf[AdminMenuServiceImpl])
trait AdminMenuService extends MenuService {
  def save(menuData: ApiMenu, user: String): ApiMenu
}

@Singleton
class AdminMenuServiceImpl @Inject()(config: Configuration,
                                     environment: Environment,
                                     s3: S3Client) extends MenuServiceRepository(config, environment.mode) with AdminMenuService {

  import de.welt.contentapi.menu.models.MenuFormats._

  override def get(): ApiMenu = loadMenu(s3)
    .getOrElse(throw new IllegalStateException(s"There was an error to load the menu json file from S3. Check if the config is correct: $s3Config"))

  override def save(menuData: ApiMenu, user: String): ApiMenu = {
    if (menuData.isEmpty) {
      throw new IllegalStateException(
        """
          |It is not allowed to save a empty menu. Please check your input data.
          |When this is not an error fix the json file on S3 by hand. Sorry.
        """.stripMargin)
    } else {
      val menuDataWithUpdatedMetadata: ApiMenu = menuData.copy(metadata = ApiMenuMetadata(changedBy = user))
      val serializedMenuData = Json.toJson(menuDataWithUpdatedMetadata).toString()

      Logger.info(s"saving menu data to s3 -> ${s3Config.fullFilePath} - user: $user")

      s3.putPrivate(s3Config.bucket, s3Config.fullFilePath, serializedMenuData, "application/json")

      menuDataWithUpdatedMetadata
    }
  }
}
