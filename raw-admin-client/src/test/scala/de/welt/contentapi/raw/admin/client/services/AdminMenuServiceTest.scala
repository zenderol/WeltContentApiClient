package de.welt.contentapi.raw.admin.client.services

import de.welt.contentapi.core.client.services.s3.S3Client
import de.welt.contentapi.raw.models.RawMenu
import de.welt.contentapi.utils.Env.{Env, Live}
import org.mockito.Matchers
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import play.api.libs.json.Json

class AdminMenuServiceTest extends PlaySpec with MockitoSugar {

  trait Fixture {
    val s3: S3Client = mock[S3Client]
    val configData = Map(
      AdminMenuServiceImpl.bucketConfigKey → "bucket",
      AdminMenuServiceImpl.folderConfigKey → "folder",
      AdminMenuServiceImpl.fileConfigKey → "file"
    )
    private val configuration: Configuration = Configuration.from(configData)
    val amsService = new AdminMenuServiceImpl(configuration, s3)
  }

  "AdminMenuService" should {

    "persist menu data on S3" in new Fixture {
      implicit val env: Env = Live

      val root: RawMenu = RawMenu()

      amsService.save(root, "Dick Butt")

      val bucket: String = configData.getOrElse(AdminMenuServiceImpl.bucketConfigKey, "")
      val file: String = configData.getOrElse(AdminMenuServiceImpl.folderConfigKey, "")
      verify(s3).putPrivate(Matchers.eq(bucket), startsWith(file), anyString(), contains("json"))
    }

    "get menu data from S3" in new Fixture {

      import de.welt.contentapi.raw.models.RawWrites._

      implicit val env: Env = Live

      val menuDataOnS3: RawMenu = RawMenu()
      private val json = Json.toJson(menuDataOnS3).toString
      when(s3.get(Matchers.anyString(), Matchers.anyString())) thenReturn Some(json)

      val menuData: RawMenu = amsService.get()

      menuData mustEqual menuDataOnS3
    }
  }

}
