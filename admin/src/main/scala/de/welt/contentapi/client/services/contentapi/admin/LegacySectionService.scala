package de.welt.contentapi.client.services.contentapi.admin

import com.google.inject.{Inject, Singleton}
import de.welt.contentapi.admin.models.{SdpSectionData, SdpSectionDataReads}
import de.welt.contentapi.client.services.configuration.ContentClientConfig
import de.welt.contentapi.client.services.s3.S3
import de.welt.contentapi.core.traits.Loggable
import play.api.cache.CacheApi
import play.api.libs.json.{JsError, JsSuccess, Json}

import scala.concurrent.duration._

trait LegacySectionService {

  def getSectionData: SdpSectionData

}

@Singleton
class LegacySectionServiceImpl @Inject()(s3: S3, cache: CacheApi, funkConfig: ContentClientConfig)
  extends LegacySectionService with Loggable {

  import SdpSectionDataReads._

  lazy val file = funkConfig.aws.s3.sectionMetadata.file
  lazy val bucket = funkConfig.aws.s3.sectionMetadata.bucket

  override def getSectionData: SdpSectionData = cache.getOrElse("s3-section-data", 2.hours) {

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
