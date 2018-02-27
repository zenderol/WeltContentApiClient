package de.welt.contentapi.core.client.services.s3

import java.time.Instant
import javax.inject.{Inject, Singleton}

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.model._
import com.amazonaws.services.s3.{AmazonS3, AmazonS3Client}
import com.amazonaws.util.StringInputStream
import com.google.inject.ImplementedBy
import play.api.{Configuration, Environment, Logger}

import scala.io.{Codec, Source}

@ImplementedBy(classOf[S3ClientImpl])
sealed trait S3Client {

  val config: Configuration
  val environment: Environment
  implicit val codec: Codec = Codec.UTF8

  lazy val client: AmazonS3 = {
    val region: Regions = Regions.fromName(config.get[String](S3ClientConstants.RegionConfigKey))

    Logger.debug(s"s3 connected to $region")

    (environment.mode match {
      case play.api.Mode.Prod ⇒ AmazonS3Client.builder()
      case _ ⇒ AmazonS3Client.builder()
        .withCredentials(
          new AWSStaticCredentialsProvider(
            new BasicAWSCredentials(
              config.get[String]("welt.aws.s3.dev.accessKey"),
              config.get[String]("welt.aws.s3.dev.secretKey")
            )
          )
        )
    }).withRegion(region).build()
  }

  def get(bucket: String, key: String): Option[String] = withS3Result(bucket, key)({
    Logger.debug(s"get($key)")
    result ⇒ Source.fromInputStream(result.getObjectContent).mkString
  })

  def getWithLastModified(bucket: String, key: String): Option[(String, Instant)] = withS3Result(bucket, key)({
    result ⇒
      val content = Source.fromInputStream(result.getObjectContent).mkString
      val lastModified = result.getObjectMetadata.getLastModified.toInstant
      (content, lastModified)
  })

  def getLastModified(bucket: String, key: String): Option[Instant] = {
    val request = new GetObjectMetadataRequest(bucket, key)
    try {
      Some(client.getObjectMetadata(request).getLastModified.toInstant)
    } catch {
      case _: Exception ⇒ None
    }
  }

  def putPrivate(bucket: String, key: String, value: String, contentType: String): Unit = {
    //    log.info(s"put($key, $value, $contentType)")
    put(bucket, key, value, contentType)
  }

  private def withS3Result[T](bucket: String, key: String)(action: S3Object ⇒ T): Option[T] =
    try {
      val request = new GetObjectRequest(bucket, key)
      val result = client.getObject(request)

      // http://stackoverflow.com/questions/17782937/connectionpooltimeoutexception-when-iterating-objects-in-s3
      try {
        Some(action(result))
      } finally {
        result.close()
      }
    } catch {
      case e: AmazonS3Exception if e.getStatusCode == 404 ⇒
        Logger.debug("not found at %s - %s" format(bucket, key))
        None
    }


  private def put(bucket: String, key: String, value: String, contentType: String) {
    val metadata = new ObjectMetadata()
    metadata.setContentType(contentType)
    metadata.setContentLength(value.getBytes("UTF-8").length)

    val request = new PutObjectRequest(bucket, key, new StringInputStream(value), metadata)
      .withCannedAcl(CannedAccessControlList.Private)


    client.putObject(request)
  }
}

@Singleton
class S3ClientImpl @Inject()(override val config: Configuration, override val environment: Environment) extends S3Client {}

object S3ClientConstants {
  protected[s3] val RegionConfigKey = "welt.aws.s3.region"
}
