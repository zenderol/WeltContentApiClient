package de.welt.contentapi.raw.client.services

import com.google.inject.{Inject, Singleton}
import de.welt.contentapi.core.client.services.s3.S3Client
import de.welt.contentapi.raw.client.models.{SdpSectionData, SdpSectionDataReads}
import de.welt.contentapi.utils.Loggable
import play.api.Configuration
import play.api.cache.CacheApi
import play.api.libs.json.{JsError, JsSuccess, Json}

import scala.concurrent.duration._

trait SdpSectionDataService {

  def getSectionData: SdpSectionData

}

@Singleton
class SdpSectionDataServiceImpl @Inject()(s3: S3Client, cache: CacheApi, configuration: Configuration)
  extends SdpSectionDataService with Loggable {

  import SdpSectionDataReads._

  lazy val file = configuration.getString("welt.aws.s3.sdp.file")
  lazy val bucket = configuration.getString("welt.aws.s3.sdp.bucket")

  override def getSectionData: SdpSectionData = cache.getOrElse("s3-section-data", 15.minutes) {

    val data = for {
      b ← bucket
      f ← file
    } yield s3.get(b, f) match {
      case Some(response) ⇒ // parse that shit
        val validationResult = Json.parse(response)
        validationResult.validate[SdpSectionData] match {
          case JsSuccess(value, _) ⇒
            log.debug("S3 Section Data successfully loaded.")
            value
          case err@JsError(e) ⇒
            log.error("S3 Section Data could not be parsed.", new scala.IllegalArgumentException(err.errors.head.toString))
            SdpSectionData("/", "No Data", None, Seq.empty, -1)
        }
      case _ ⇒
        log.error(s"S3 Section Data not found: '$b/$f'")
        SdpSectionData("/", "No Data", None, Seq.empty, -1)
    }
    data getOrElse SdpSectionData("/", "No Data", None, Seq.empty, -1)
  }

}
