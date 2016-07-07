package de.welt.contentapi.client.services.contentapi

import java.time.Instant
import javax.inject.{Inject, Singleton}

import de.welt.contentapi.core.models.api.ApiContent
import de.welt.contentapi.core.models.config.{Channel, ChannelData, ChannelId}
import de.welt.contentapi.client.services.configuration.ContentClientConfig
import de.welt.contentapi.client.services.s3.S3
import de.welt.contentapi.core.traits.Loggable
import play.api.cache.CacheApi
import play.api.libs.json.{JsError, JsSuccess, Json}

import scala.concurrent.duration._

case class SdpSectionData(url: String,
                          displayName: String,
                          lastModifiedDate: Option[String],
                          children: Seq[SdpSectionData]) {

  private def defineAdTag(data: ChannelData) = data.copy(adData = data.adData.copy(definesAdTag = true))

  def toChannel: Channel = {
    val root = transform
    // initially set hasAdTag to true for level 0 & 1 of the section tree
    root.data = defineAdTag(root.data)
    root.children.foreach { child ⇒ child.data = defineAdTag(child.data) }

    // set the parent-relation for all elements
    root.updateParentRelations()
    root
  }

  private def transform: Channel = Channel(
    id = ChannelId(url),
    data = ChannelData(displayName),
    children = children.map(_.toChannel),
    lastModifiedDate = lastModifiedDate match {
      case Some("") ⇒ Instant.now.toEpochMilli
      case Some(s) ⇒ s.toLong
      case _ ⇒ Instant.now.toEpochMilli
    }
  )

}

object SdpSectionDataReads {

  import play.api.libs.functional.syntax._
  import play.api.libs.json.Reads._
  import play.api.libs.json._

  // Combinator syntax

  implicit lazy val s3SectionDataReads: Reads[SdpSectionData] = (
    (JsPath \ "url").read[String] and
      (JsPath \ "displayName").read[String] and
      (JsPath \ "lastModifiedDate").readNullable[String] and
      (JsPath \ "children").lazyRead(Reads.seq[SdpSectionData](s3SectionDataReads))
    ) (SdpSectionData)
}

trait LegacySectionService {

  def getSectionData: SdpSectionData

  def byPath(path: String): Option[SdpSectionData]

  def enrich(apiContent: ApiContent): EnrichedApiContent
}

@Singleton
class LegacySectionServiceImpl @Inject()(s3: S3, cache: CacheApi, funkConfig: ContentClientConfig)
  extends LegacySectionService with Loggable {

  import SdpSectionDataReads._

  lazy val file = funkConfig.aws.s3.sectionMetadata.file
  lazy val bucket = funkConfig.aws.s3.sectionMetadata.bucket

  def sectionData: SdpSectionData = cache.getOrElse("s3-section-data", 2.hours) {

    val data = for {
      b <- bucket
      f <- file
    } yield s3.get(b, f) match {
      case Some(response) => // parse that shit
        val validationResult = Json.parse(response)
        validationResult.validate[SdpSectionData] match {
          case JsSuccess(value, _) =>
            log.debug("S3 Section Data successfully loaded.")
            value
          case err@JsError(e) =>
            log.error("S3 Section Data could not be parsed.", new scala.IllegalArgumentException(err.errors.head.toString))
            SdpSectionData("/", "No Data", None, Seq.empty)
        }
      case _ =>
        log.error(s"S3 Section Data not found: '$b/$f'")
        SdpSectionData("/", "No Data", None, Seq.empty)
    }
    data getOrElse SdpSectionData("/", "No Data", None, Seq.empty)
  }

  override def byPath(path: String): Option[SdpSectionData] = {
    None // todo
  }

  override def enrich(apiContent: ApiContent): EnrichedApiContent = {

    val maybeSectionData = apiContent.sections.flatMap { sectionData =>
      sectionData.home.flatMap { home => {
        byPath(home)
      }}
    }
    EnrichedApiContent(apiContent, maybeSectionData)
  }

  override def getSectionData: SdpSectionData = sectionData
}
