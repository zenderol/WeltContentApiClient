package de.welt.contentapi.raw.models

import java.io.PrintWriter

import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.GetObjectRequest
import de.welt.contentapi.raw.models.legacy.Datasource.{CuratedSource, SearchSource}
import de.welt.contentapi.raw.models.legacy.Query.{FlagQuery, SectionQuery, SubTypeQuery, TypeQuery}
import de.welt.contentapi.raw.models.legacy._
import org.scalatest.Ignore
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{Format, JsError, JsObject, JsResult, JsSuccess, JsValue, Json, Reads}

import scala.io.Source

@Ignore
class MigrationTest extends PlaySpec {

  "Migrate" should {

    val s3Client = new AmazonS3Client()
    s3Client.setEndpoint("s3.eu-central-1.amazonaws.com")

    val getRequest = new GetObjectRequest("up-production-front-end-configuration-eu-central-1", "janus2/prod/Live/config.json")

    "load contents from s3" in {

      val result = s3Client.getObject(getRequest)
      try {
        val content = Source.fromInputStream(result.getObjectContent).mkString
        val apiChannel = Json.parse(content).validate[ApiChannel](reads.FullChannelReads.channelReads) match {
          case JsSuccess(apiCh, _) => apiCh
          case err@JsError(_) => throw new IllegalStateException(err.toString)
        }
        log.info(apiChannel.toString)
        val rawChannel = ApiChannelToRawChannelConverter(apiChannel)

        val json = Json.toJson(rawChannel)(FullRawChannelWrites.channelWrites)

        new PrintWriter("/tmp/migrated.json") {
          write(Json.prettyPrint(json))
          close()
        }
        /**
          * If you have write permissions:
          *
          * aws s3 cp --region eu-central-1 /tmp/migrated.json s3://up-production-front-end-configuration-eu-central-1/rawTree/dev/Live/config.json
          */
        log.info("File written to /tmp/migrated.json")
      } finally {
        result.close()
      }

      //      ApiChannelToRawChannelConverter(null)

    }
  }

}

object reads {

  object FullChannelReads {

    import SimpleFormats._
    import StageFormats._
    import play.api.libs.functional.syntax._
    import play.api.libs.json._

    implicit lazy val channelReads: Reads[ApiChannel] = (
      (__ \ "id").read[ChannelId] and
        (__ \ "data").read[ApiChannelData] and
        (__ \ "stages").readNullable[Seq[Stage]] and
        (__ \ "parent").lazyRead(Reads.optionWithNull(channelReads)) and
        (__ \ "children").lazyRead(Reads.seq[ApiChannel](channelReads)) and
        (__ \ "hasChildren").read[Boolean] and
        (__ \ "lastModifiedDate").read[Long] and
        (__ \ "metadata").readNullable[ApiChannelMetadataNew]
      ) (ApiChannel)
  }

  object PartialChannelReads {

    import SimpleFormats._
    import de.welt.contentapi.raw.models.legacy.StageFormats._

    implicit lazy val noChildrenReads: Reads[ApiChannel] = new Reads[ApiChannel] {
      override def reads(json: JsValue): JsResult[ApiChannel] = json match {
        case JsObject(underlying) ⇒ (for {
          id ← underlying.get("id").map(_.as[ChannelId])
          data ← underlying.get("data").map(_.as[ApiChannelData])
        } yield JsSuccess(
          ApiChannel(
            id = id,
            data = data,
            stages = underlying.get("stages").flatMap(_.asOpt[Seq[Stage]]),
            metadata = underlying.get("metadata").flatMap(_.asOpt[ApiChannelMetadataNew]),
            children = Seq.empty
          )))
          .getOrElse(JsError("Could not validate json [something is missing]. " + Json.prettyPrint(json)))

        case err@_ ⇒ JsError(s"expected js-object, but was $err")
      }
    }
  }

}

object SimpleFormats {

  implicit lazy val idFormat: Format[ChannelId] = Json.format[ChannelId]
  implicit lazy val dataFormat: Format[ApiChannelData] = Json.format[ApiChannelData]
  implicit lazy val metaDataFormat: Format[ApiChannelMetadata] = Json.format[ApiChannelMetadata]
  implicit lazy val channelThemeFormat: Format[ApiChannelTheme] = Json.format[ApiChannelTheme]
  implicit lazy val adFormat: Format[ApiChannelAdData] = Json.format[ApiChannelAdData]
  implicit lazy val metaDataNewFormat: Format[ApiChannelMetadataNew] = Json.format[ApiChannelMetadataNew]

}

object StageFormats {

  import play.api.libs.json._

  // need typesafe val, because default Type is OFormat[...]
  implicit lazy val commercialFormat: Format[ApiCommercial] = Json.format[ApiCommercial]
  implicit lazy val sectionReferenceFormat: Format[ApiSectionReference] = Json.format[ApiSectionReference]
  implicit lazy val headlineThemeFormat: Format[ApiHeadlineTheme] = Json.format[ApiHeadlineTheme]
  implicit lazy val stageThemeFormat: Format[ApiStageTheme] = Json.format[ApiStageTheme]
  implicit lazy val stageConfigFormat: Format[ApiStageConfig] = Json.format[ApiStageConfig]
  implicit lazy val stageGroupFormat: Format[ApiStageGroup] = Json.format[ApiStageGroup]
  implicit lazy val stageFormat: Format[Stage] = Json.format[Stage]
  // Data Sources
  implicit lazy val datasourceFormat: Format[Datasource] = Json.format[Datasource]
  implicit lazy val searchSourceFormat: Format[SearchSource] = Json.format[SearchSource]
  implicit lazy val curatedSourceFormat: Format[CuratedSource] = Json.format[CuratedSource]
  // SearchApiFilters
  implicit lazy val queryFormat: Format[Query] = Json.format[Query]
  implicit lazy val flagQueryFormat: Format[FlagQuery] = Json.format[FlagQuery]
  implicit lazy val sectionQueryFormat: Format[SectionQuery] = Json.format[SectionQuery]
  implicit lazy val subtypeQueryFormat: Format[SubTypeQuery] = Json.format[SubTypeQuery]
  implicit lazy val typeQueryFormat: Format[TypeQuery] = Json.format[TypeQuery]
}