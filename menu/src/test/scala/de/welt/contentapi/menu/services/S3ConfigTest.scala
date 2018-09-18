package de.welt.contentapi.menu.services

import org.scalatestplus.play.PlaySpec
import play.api.Mode
import play.api.Configuration

class S3ConfigTest extends PlaySpec {

  "S3 Config" should {

    val defaultConfig: Configuration = Configuration.from(data = Map(
      MenuConfig.BucketConfigKey → "bucket",
      MenuConfig.FolderConfigKey → "folder",
      MenuConfig.FileConfigKey → "file"
    ))

    val modeConfig: Configuration = Configuration.from(data = Map(
      MenuConfig.ModeConfigKey → "mode"
    ))

    "read the values from Configuration" in {
      val config: S3Config = S3Config(defaultConfig, Mode.Prod)

      config.fullFilePath mustEqual "folder/prod/file"
    }

    "use dev folder in dev mode" in {
      val config: S3Config = S3Config(defaultConfig, Mode.Dev)

      config.fullFilePath mustEqual "folder/dev/file"
    }

    """|override the env folder with config mode
       |This is for testing purpose. E.g. read prod data from localhost (Mode.Dev)""".stripMargin in {
      val config: S3Config = S3Config(defaultConfig ++ modeConfig, Mode.Dev)

      config.fullFilePath mustEqual "folder/mode/file"
    }

    "not override env folder from config mode in production mode" in {
      val config: S3Config = S3Config(defaultConfig ++ modeConfig, Mode.Prod)

      config.fullFilePath mustEqual "folder/prod/file"
    }

  }

}
