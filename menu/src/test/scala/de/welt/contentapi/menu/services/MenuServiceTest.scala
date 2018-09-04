package de.welt.contentapi.menu.services

import java.time.Instant

import de.welt.contentapi.core.client.TestExecutionContext
import de.welt.contentapi.core.client.services.s3.S3Client
import de.welt.contentapi.menu.models.ApiMenu
import org.mockito.Mockito._
import org.mockito.{Matchers, Mockito}
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.{Configuration, Environment, Mode}

class MenuServiceTest extends PlaySpec with MockitoSugar {

  trait Fixture {
    val s3: S3Client = mock[S3Client]
    val configuration = Configuration(
      MenuConfig.bucketConfigKey → "bucket",
      MenuConfig.folderConfigKey → "folder",
      MenuConfig.fileConfigKey → "file"
    )
  }

  "MenuService" should {

    "not load any data when in test mode" in new Fixture {
      val menuService = new MenuServiceImpl(configuration, s3, Environment.simple(), TestExecutionContext.executionContext)
      Mockito.verify(s3, Mockito.never()).getLastModified("bucket", "file.key")
    }

    "get menu data when not in test mode" in new Fixture {

      import de.welt.contentapi.menu.models.MenuFormats._

      val menuDataOnS3: ApiMenu = ApiMenu()
      val json: String = Json.toJson(menuDataOnS3).toString

      when(s3.get(Matchers.anyString(), Matchers.anyString())) thenReturn Some(json)
      when(s3.getLastModified(Matchers.anyString(), Matchers.anyString())) thenReturn Some(Instant.now)

      val menuService = new MenuServiceImpl(
        configuration,
        s3,
        Environment.simple(mode = Mode.Dev),
        TestExecutionContext.executionContext
      )

      menuService.get() mustEqual menuDataOnS3
    }
  }

}
