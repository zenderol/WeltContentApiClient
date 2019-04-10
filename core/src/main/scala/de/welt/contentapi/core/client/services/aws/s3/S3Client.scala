package de.welt.contentapi.core.client.services.aws.s3

import java.time.Instant

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model._
import com.amazonaws.util.StringInputStream
import com.google.inject.{Inject, Singleton}
import de.welt.contentapi.utils.Loggable

import scala.io.{Codec, Source}

@Singleton
class S3Client @Inject()(client: AmazonS3) extends Loggable {

  implicit val codec: Codec = Codec.UTF8

  def get(bucket: String, key: String): Option[String] = withS3Result(bucket, key)({
    log.debug(s"get($key)")
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
    log.debug(s"put($key, ***, $contentType)")
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
        log.debug("not found at %s - %s" format(bucket, key))
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