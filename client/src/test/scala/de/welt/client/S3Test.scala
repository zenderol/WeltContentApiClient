package de.welt.client

import java.io.File

import de.welt.contentapi.client.services.configuration.ContentClientConfigImpl
import de.welt.contentapi.client.services.s3.S3Impl
import org.scalatestplus.play.PlaySpec
import play.api.{Configuration, Environment, Mode}

class S3Test extends PlaySpec {

  val prodEnv = Environment(new File("."), getClass.getClassLoader, Mode.Prod)
  val devEnv = Environment(new File("."), getClass.getClassLoader, Mode.Dev)

  val s3Config: Configuration = Configuration(
    "funkotron.aws.s3.enabled" -> true,
    "funkotron.aws.s3.endpoint" -> "s3.eu-central-1.amazonaws.com"
  )

  class FunkConfigTestImpl(override val configuration: Configuration) extends ContentClientConfigImpl(configuration)

  "S3" should {

    "return None if s3 is not enabled in config" in {

      val config = new FunkConfigTestImpl(s3Config ++ Configuration("funkotron.aws.s3.enabled" -> false))
      val s3 = new S3Impl(funkConfig = config)

      s3.client mustBe defined
    }

    "return None if there is no s3 endpoint set" in {

      val config = new FunkConfigTestImpl(Configuration("funkotron.aws.s3.enabled" -> true))
      val s3 = new S3Impl(funkConfig = config)

      s3.client mustBe None
    }

    "create a s3 client in Prod mode when enabled and endpoint set" in {
      val config = new FunkConfigTestImpl(s3Config)
      val s3 = new S3Impl(funkConfig = config)

      s3.client mustBe defined
    }
  }
}
