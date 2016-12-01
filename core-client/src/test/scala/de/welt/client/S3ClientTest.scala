package de.welt.client

import java.io.File

import com.amazonaws.services.s3.AmazonS3Client
import de.welt.contentapi.core.client.services.exceptions.BadConfigurationException
import de.welt.contentapi.core.client.services.s3.S3ClientImpl
import org.scalatestplus.play.PlaySpec
import play.api.{Configuration, Environment, Mode}

class S3ClientTest extends PlaySpec {

  val prodEnv = Environment(new File("."), getClass.getClassLoader, Mode.Prod)
  val devEnv = Environment(new File("."), getClass.getClassLoader, Mode.Dev)

  "S3" should {

    "return s3Client if s3 endpoint is set in config" in {
      val s3 = new S3ClientImpl(config = Configuration("welt.aws.s3.endpoint" -> "s3.eu-central-1.amazonaws.com"))
      s3.client mustBe an [AmazonS3Client]
    }

    "throw BadConfigurationException without config for s3 endpoint" in {
      an [BadConfigurationException] should be thrownBy new S3ClientImpl(config = Configuration())
    }
  }
}
