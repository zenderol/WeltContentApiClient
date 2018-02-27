package de.welt.contentapi.core.client.services.s3

import java.io.File

import com.amazonaws.services.s3.{AmazonS3, AmazonS3Client}
import com.typesafe.config.ConfigException
import org.scalatestplus.play.PlaySpec
import play.api.{Configuration, Environment, Mode}

class S3ClientTest extends PlaySpec {

  val prodEnv: Environment = Environment.simple(new File("."), Mode.Prod)
  val devEnv: Environment = Environment.simple(new File("."), Mode.Dev)

  "S3" should {

    "Prod Mode" should {

      val prodEnv = Environment.simple(mode = Mode.Prod)

      "return s3Client if s3 endpoint is set" in {
        val s3 = new S3ClientImpl(
          config = Configuration(S3ClientConstants.RegionConfigKey → "eu-central-1"),
          environment = prodEnv
        )

        s3.client mustBe an[AmazonS3]
      }

      "throw BadConfigurationException without config for s3 endpoint in Prod Mode" in {
        an[ConfigException.Missing] should be thrownBy new S3ClientImpl(
          config = Configuration(),
          environment = prodEnv
        ).client
      }

    }

    "Dev Mode" should {

      val devEnv = Environment.simple(mode = Mode.Dev)

      "return s3Client if s3 endpoint and AWS credentials are set" in {
        val s3 = new S3ClientImpl(
          config = Configuration(
            S3ClientConstants.RegionConfigKey → "eu-central-1",
            "welt.aws.s3.dev.accessKey" → "accessKey",
            "welt.aws.s3.dev.secretKey" → "secretKey"
          ),
          environment = devEnv
        )
        s3.client mustBe an[AmazonS3Client]
      }

      "throw BadConfigurationException without config for s3 AWS credentials are not set" in {
        an[ConfigException.Missing] should be thrownBy new S3ClientImpl(
          config = Configuration("welt.aws.s3.endpoint" → "s3.eu-central-1.amazonaws.com"),
          environment = devEnv
        ).client
      }
    }

  }
}
