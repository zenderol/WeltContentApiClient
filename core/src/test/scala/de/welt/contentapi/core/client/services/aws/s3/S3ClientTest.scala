package de.welt.contentapi.core.client.services.aws.s3

import java.io.ByteArrayInputStream
import java.nio.charset.Charset

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.{GetObjectRequest, S3Object, S3ObjectInputStream}
import org.mockito.{ArgumentMatchers, Mockito}
import org.scalatestplus.play.PlaySpec

class S3ClientTest extends PlaySpec {

  trait Scope {
    val s3: AmazonS3 = Mockito.mock(classOf[AmazonS3])
    val response: S3Object = Mockito.mock(classOf[S3Object])
    val service: S3Client = new S3Client(s3)
  }


  "S3Client" should {
    "pass requests to the underlying amazon client" in new Scope {
      Mockito.when(s3.getObject(ArgumentMatchers.any[GetObjectRequest]))
        .thenReturn(response)

      //noinspection ScalaStyle
      Mockito.when(response.getObjectContent)
        .thenReturn(new S3ObjectInputStream(new ByteArrayInputStream("hello, world!".getBytes(Charset.defaultCharset())), null))
      val s3_response = service.get("foo", "bar")

      s3_response mustBe Some("hello, world!")
    }
  }
}
