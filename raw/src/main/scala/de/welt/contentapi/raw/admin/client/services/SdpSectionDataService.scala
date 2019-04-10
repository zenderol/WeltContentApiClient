package de.welt.contentapi.raw.admin.client.services

import javax.inject.{Inject, Singleton}
import com.google.inject.ImplementedBy
import de.welt.contentapi.core.client.services.aws.s3.S3Client
import de.welt.contentapi.raw.admin.client.models.SdpSectionData
import de.welt.contentapi.raw.admin.client.models.SdpSectionDataReads.s3SectionDataReads
import de.welt.contentapi.utils.Loggable
import play.api.cache.SyncCacheApi
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.{Configuration, Logger}

import scala.concurrent.duration._

@ImplementedBy(classOf[SdpSectionDataServiceImpl])
trait SdpSectionDataService {

  def getSectionData: SdpSectionData

}

@Singleton
class SdpSectionDataServiceImpl @Inject()(s3: S3Client, cache: SyncCacheApi, configuration: Configuration)
  extends SdpSectionDataService with Loggable {

  override def getSectionData: SdpSectionData = cache.getOrElseUpdate("s3-section-data", 15.minutes) {

    val data = for {
      b ← configuration.getOptional[String]("welt.aws.s3.sdp.bucket")
      f ← configuration.getOptional[String]("welt.aws.s3.sdp.file")
      response ← s3.get(b, f)
    } yield {
      val validationResult = Json.parse(response)
      validationResult.validate[SdpSectionData] match {
        case JsSuccess(value, _) ⇒
          log.debug("S3 Section Data successfully loaded.")
          value
        case err@JsError(e) ⇒
          log.error("S3 Section Data could not be parsed.", new scala.IllegalArgumentException(err.errors.head.toString))
          SdpSectionData("/", "No Data", None, Seq.empty, -1)
      }
    }
    data.getOrElse {
      val context = configuration.getOptional[Configuration]("welt.aws.s3.sdp").map(_.toString).getOrElse("S3 not configured")
      log.error(s"S3 Section Data not found: $context")
      SdpSectionData("/", "No Data", None, Seq.empty, -1)
    }
  }
}
