package de.welt.client

import java.io.File

import de.welt.contentapi.core.client.services.s3.S3ClientImpl
import org.scalatestplus.play.PlaySpec
import play.api.{Configuration, Environment, Mode}

class S3ClientTest extends PlaySpec {

  val prodEnv = Environment(new File("."), getClass.getClassLoader, Mode.Prod)
  val devEnv = Environment(new File("."), getClass.getClassLoader, Mode.Dev)

  "S3" should {

    "return None if s3 is not enabled in config" in {

      val s3 = new S3ClientImpl(config = Configuration("welt.aws.s3.endpoint" -> "s3.eu-central-1.amazonaws.com"))

      s3.client mustBe defined
    }

    "return None if there is no s3 endpoint set" in {

      val s3 = new S3ClientImpl(config = Configuration())

      s3.client mustBe None
    }

  }
}
