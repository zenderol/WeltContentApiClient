package de.welt.contentapi.menu.services

import de.welt.contentapi.core.client.services.s3.S3Client
import de.welt.contentapi.core.models.ApiReference
import de.welt.contentapi.menu.models.{ApiMenu, ApiMenuLink}
import org.mockito.Matchers
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.{Configuration, Environment}

class AdminMenuServiceTest extends PlaySpec with MockitoSugar {

  trait Fixture {
    val s3: S3Client = mock[S3Client]
    val configData = Map(
      MenuConfig.bucketConfigKey → "bucket",
      MenuConfig.folderConfigKey → "folder",
      MenuConfig.fileConfigKey → "file"
    )
    val configuration: Configuration = Configuration.from(configData)
    val adminMenuService = new AdminMenuServiceImpl(configuration, Environment.simple(), s3)
  }

  "AdminMenuService" should {

    "persist menu data on S3" in new Fixture {
      val root: ApiMenu = ApiMenu(
        primaryMenu = Seq(
          ApiMenuLink(
            reference = ApiReference(label = Some("foo"), href = Some("/foo"))
          )
        ),
        secondaryMenu = Seq(
          ApiMenuLink(
            reference = ApiReference(label = Some("bar"), href = Some("/bar"))
          )
        )
      )

      adminMenuService.save(root, "Dick Butt")

      val bucket: String = configData.getOrElse(MenuConfig.bucketConfigKey, "")
      val file: String = configData.getOrElse(MenuConfig.folderConfigKey, "")
      verify(s3).putPrivate(Matchers.eq(bucket), startsWith(file), anyString(), contains("json"))
    }

    "throw an error when saved menu is empty. Preventing clearing the menu by accident." in new Fixture {
      val empty: ApiMenu = ApiMenu()

      assertThrows[IllegalStateException] {
        adminMenuService.save(empty, "Dick Butt")
      }
    }

    "get menu data from S3" in new Fixture {

      import de.welt.contentapi.menu.models.MenuFormats._

      val menuDataOnS3: ApiMenu = ApiMenu()
      private val json = Json.toJson(menuDataOnS3).toString
      when(s3.get(Matchers.anyString(), Matchers.anyString())) thenReturn Some(json)

      val menuData: ApiMenu = adminMenuService.get()

      menuData mustBe menuDataOnS3
    }

    "throw an error when getting no json from S3. In case of invalid / corrupt data or invalid S3 configuration." in new Fixture {
      when(s3.get(Matchers.anyString(), Matchers.anyString())) thenReturn None

      assertThrows[IllegalStateException] {
        adminMenuService.get()
      }

    }
  }

}
