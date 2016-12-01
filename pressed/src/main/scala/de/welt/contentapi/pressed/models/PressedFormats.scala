package de.welt.contentapi.pressed.models

import de.welt.contentapi.core.models.{ApiContent, ApiReference}

import play.api.libs.json._
import play.api.libs.functional.syntax._



object PressedReads {
  import de.welt.contentapi.core.models.ApiReads._
  implicit lazy val apiTeaserConfigReads = Json.reads[ApiTeaserConfig]
  implicit lazy val apiChannelReads = Json.reads[ApiChannel]
  implicit lazy val apiStageConfigurationReads = Json.reads[ApiStageConfiguration]
  implicit lazy val apiCommercialConfigurationReads = Json.reads[ApiCommercialConfiguration]
  implicit lazy val apiThemeConfigurationReads = Json.reads[ApiThemeConfiguration]
  implicit lazy val apiHeaderConfigurationReads = Json.reads[ApiHeaderConfiguration]
  implicit lazy val apiBrandingConfigurationReads = Json.reads[ApiSponsoringConfiguration]
  implicit lazy val apiMetaRobotsReads = Json.reads[ApiMetaRobots]
  implicit lazy val apiMetaConfigurationReads = Json.reads[ApiMetaConfiguration]
  implicit lazy val apiConfigurationReads = Json.reads[ApiConfiguration]

  implicit lazy val apiPressedContentReads: Reads[ApiPressedContent] = new Reads[ApiPressedContent] {
    override def reads(json: JsValue): JsResult[ApiPressedContent] = json match {
      case JsObject(underlying) ⇒ (for {
        content ← underlying.get("content").map(_.as[ApiContent])
        related ← underlying.get("related").map(_.asOpt[Seq[ApiPressedContent]])
        channel ← underlying.get("channel").map(_.asOpt[ApiChannel])
        configuration ← underlying.get("configuration").map(_.asOpt[ApiConfiguration])
      } yield JsSuccess(
        ApiPressedContent(
          content = content,
          related = related,
          channel = channel,
          configuration = configuration
          )))
        .getOrElse(JsError("Could not validate json [something is missing]. " + Json.prettyPrint(json)))

      case err@_ ⇒ JsError(s"expected js-object, but was $err")
    }
  }

  implicit lazy val apiTeaserReads = Json.reads[ApiTeaser]
  implicit lazy val apiStageReads = Json.reads[ApiStage]
  implicit lazy val apiPressedSectionReads: Reads[ApiPressedSection] = Json.reads[ApiPressedSection]
}

object PressedWrites {
  import de.welt.contentapi.core.models.ApiWrites._
  implicit lazy val apiTeaserConfigWrites = Json.writes[ApiTeaserConfig]
  implicit lazy val apiTeaserWrites = Json.writes[ApiTeaser]
  implicit lazy val apiChannelWrites = Json.writes[ApiChannel]
  implicit lazy val apiStageConfigurationWrites = Json.writes[ApiStageConfiguration]
  implicit lazy val apiCommercialConfigurationWrites = Json.writes[ApiCommercialConfiguration]
  implicit lazy val apiThemeConfigurationWrites = Json.writes[ApiThemeConfiguration]
  implicit lazy val apiHeaderConfigurationWrites = Json.writes[ApiHeaderConfiguration]
  implicit lazy val apiBrandingConfigurationWrites = Json.writes[ApiSponsoringConfiguration]
  implicit lazy val apiMetaRobotsWrites = Json.writes[ApiMetaRobots]
  implicit lazy val apiMetaConfigurationWrites = Json.writes[ApiMetaConfiguration]
  implicit lazy val apiConfigurationWrites = Json.writes[ApiConfiguration]
  implicit lazy val apiPressedContentWrites: Writes[ApiPressedContent] = (
    (__ \ "content").write[ApiContent] and
      (__ \ "related").lazyWriteNullable(Writes.seq[ApiPressedContent](apiPressedContentWrites)) and
      (__ \ "channel").writeNullable[ApiChannel] and
      (__ \ "configuration").writeNullable[ApiConfiguration]
    ) (unlift(ApiPressedContent.unapply))
  implicit lazy val apiStageWrites = Json.writes[ApiStage]
  implicit lazy val apiPressedSectionWrites = Json.writes[ApiPressedSection]



}

object PressedFormats {
  import de.welt.contentapi.core.models.ApiFormats._
  import PressedReads.apiPressedContentReads
  import PressedWrites.apiPressedContentWrites
  implicit lazy val apiTeaserConfigFormat = Json.format[ApiTeaserConfig]
  implicit lazy val apiChannelFormat = Json.format[ApiChannel]
  implicit lazy val apiStageConfigurationFormat = Json.format[ApiStageConfiguration]
  implicit lazy val apiCommercialConfigurationFormat = Json.format[ApiCommercialConfiguration]
  implicit lazy val apiThemeConfigurationFormat = Json.format[ApiThemeConfiguration]
  implicit lazy val apiHeaderConfigurationFormat = Json.format[ApiHeaderConfiguration]
  implicit lazy val apiBrandingConfigurationFormat = Json.format[ApiSponsoringConfiguration]
  implicit lazy val apiMetaRobotsFormat = Json.format[ApiMetaRobots]
  implicit lazy val apiMetaConfigurationFormat = Json.format[ApiMetaConfiguration]
  implicit lazy val apiConfigurationFormat = Json.format[ApiConfiguration]
  implicit lazy val apiPressedContentFormat = Format(apiPressedContentReads, apiPressedContentWrites)
  implicit lazy val apiTeaserFormat = Json.format[ApiTeaser]
  implicit lazy val apiStageFormat = Json.format[ApiStage]
  implicit lazy val apiPressedSectionFormat = Json.format[ApiPressedSection]
}
