package de.welt.contentapi.pressed.models

import de.welt.contentapi.core.models.ApiContent
import de.welt.contentapi.pressed.models.StatusPhrase.StatusPhrase
import play.api.libs.functional.syntax._
import play.api.libs.json._


object PressedReads {

  import de.welt.contentapi.core.models.ApiReads._

  implicit lazy val apiTeaserConfigReads: Reads[ApiTeaserConfig] = Json.reads[ApiTeaserConfig]
  implicit lazy val apiChannelReads: Reads[ApiChannel] = Json.reads[ApiChannel]
  implicit lazy val apiStageConfigurationReads: Reads[ApiStageConfiguration] = Json.reads[ApiStageConfiguration]
  implicit lazy val apiCommercialTaboolaConfigurationReads: Reads[ApiCommercialTaboolaConfiguration] = Json.reads[ApiCommercialTaboolaConfiguration]
  implicit lazy val apiCommercial3rdPartyConfigurationReads: Reads[ApiCommercial3rdPartyConfiguration] = Json.reads[ApiCommercial3rdPartyConfiguration]
  implicit lazy val apiCommercialConfigurationReads: Reads[ApiCommercialConfiguration] = Json.reads[ApiCommercialConfiguration]
  implicit lazy val apiThemeConfigurationReads: Reads[ApiThemeConfiguration] = Json.reads[ApiThemeConfiguration]
  implicit lazy val apiHeaderConfigurationReads: Reads[ApiHeaderConfiguration] = Json.reads[ApiHeaderConfiguration]
  implicit lazy val apiBrandingConfigurationReads: Reads[ApiSponsoringConfiguration] = Json.reads[ApiSponsoringConfiguration]
  implicit lazy val apiMetaRobotsReads: Reads[ApiMetaRobots] = Json.reads[ApiMetaRobots]
  implicit lazy val apiMetaConfigurationReads: Reads[ApiMetaConfiguration] = Json.reads[ApiMetaConfiguration]
  implicit lazy val apiConfigurationReads: Reads[ApiConfiguration] = Json.reads[ApiConfiguration]

  implicit lazy val apiPressedContentReads: Reads[ApiPressedContent] = new Reads[ApiPressedContent] {
    override def reads(json: JsValue): JsResult[ApiPressedContent] = json match {
      case JsObject(underlying) ⇒ (for {
        content ← underlying.get("content").map(_.as[ApiContent])
      } yield JsSuccess(
        ApiPressedContent(
          content = content,
          related = underlying.get("related").map(_.as[Seq[ApiPressedContent]]),
          channel = underlying.get("channel").map(_.as[ApiChannel]),
          configuration = underlying.get("configuration").map(_.as[ApiConfiguration])
        )))
        .getOrElse(JsError("Could not validate json [something is missing]. " + Json.prettyPrint(json)))

      case err@_ ⇒ JsError(s"expected js-object, but was $err")
    }
  }

  implicit lazy val apiTeaserReads: Reads[ApiTeaser] = Json.reads[ApiTeaser]
  implicit lazy val apiStageReads: Reads[ApiStage] = Json.reads[ApiStage]
  implicit lazy val apiPressedSectionReads: Reads[ApiPressedSection] = Json.reads[ApiPressedSection]
  implicit lazy val statusPhraseReads: Reads[StatusPhrase] = Reads.enumNameReads(StatusPhrase)
  implicit lazy val apiPressedSectionResponseReads: Reads[ApiPressedSectionResponse] = Json.reads[ApiPressedSectionResponse]
}

object PressedWrites {

  import de.welt.contentapi.core.models.ApiWrites._

  implicit lazy val apiTeaserConfigWrites: Writes[ApiTeaserConfig] = Json.writes[ApiTeaserConfig]
  implicit lazy val apiTeaserWrites: Writes[ApiTeaser] = Json.writes[ApiTeaser]
  implicit lazy val apiChannelWrites: Writes[ApiChannel] = Json.writes[ApiChannel]
  implicit lazy val apiStageConfigurationWrites: Writes[ApiStageConfiguration] = Json.writes[ApiStageConfiguration]
  implicit lazy val apiCommercialTaboolaConfigurationWrites: Writes[ApiCommercialTaboolaConfiguration] = Json.writes[ApiCommercialTaboolaConfiguration]
  implicit lazy val apiCommercial3rdPartyConfigurationWrites: Writes[ApiCommercial3rdPartyConfiguration] = Json.writes[ApiCommercial3rdPartyConfiguration]
  implicit lazy val apiCommercialConfigurationWrites: Writes[ApiCommercialConfiguration] = Json.writes[ApiCommercialConfiguration]
  implicit lazy val apiThemeConfigurationWrites: Writes[ApiThemeConfiguration] = Json.writes[ApiThemeConfiguration]
  implicit lazy val apiHeaderConfigurationWrites: Writes[ApiHeaderConfiguration] = Json.writes[ApiHeaderConfiguration]
  implicit lazy val apiBrandingConfigurationWrites: Writes[ApiSponsoringConfiguration] = Json.writes[ApiSponsoringConfiguration]
  implicit lazy val apiMetaRobotsWrites: Writes[ApiMetaRobots] = Json.writes[ApiMetaRobots]
  implicit lazy val apiMetaConfigurationWrites: Writes[ApiMetaConfiguration] = Json.writes[ApiMetaConfiguration]
  implicit lazy val apiConfigurationWrites: Writes[ApiConfiguration] = Json.writes[ApiConfiguration]
  implicit lazy val apiPressedContentWrites: Writes[ApiPressedContent] = (
    (__ \ "content").write[ApiContent] and
      (__ \ "related").lazyWriteNullable(Writes.seq[ApiPressedContent](apiPressedContentWrites)) and
      (__ \ "channel").writeNullable[ApiChannel] and
      (__ \ "configuration").writeNullable[ApiConfiguration]
    ) (unlift(ApiPressedContent.unapply))
  implicit lazy val apiStageWrites: Writes[ApiStage] = Json.writes[ApiStage]
  implicit lazy val apiPressedSectionWrites: Writes[ApiPressedSection] = Json.writes[ApiPressedSection]
  implicit lazy val statusPhraseWrites: Writes[StatusPhrase] = new Writes[StatusPhrase]{
    override def writes(o: StatusPhrase): JsValue = JsString(o.toString)
  }
  implicit lazy val apiPressedSectionResponseWrites: Writes[ApiPressedSectionResponse] = Json.writes[ApiPressedSectionResponse]


}

object PressedFormats {

  import PressedReads._
  import PressedWrites._

  implicit lazy val apiTeaserConfigFormat: Format[ApiTeaserConfig] =
    Format(apiTeaserConfigReads, apiTeaserConfigWrites)

  implicit lazy val apiChannelFormat: Format[ApiChannel] =
    Format(apiChannelReads, apiChannelWrites)

  implicit lazy val apiStageConfigurationFormat: Format[ApiStageConfiguration] =
    Format(apiStageConfigurationReads, apiStageConfigurationWrites)

  implicit lazy val apiCommercialTaboolaConfigurationFormat: Format[ApiCommercialTaboolaConfiguration] =
    Format(apiCommercialTaboolaConfigurationReads, apiCommercialTaboolaConfigurationWrites)

  implicit lazy val apiCommercial3rdPartyConfigurationFormat: Format[ApiCommercial3rdPartyConfiguration] =
    Format(apiCommercial3rdPartyConfigurationReads, apiCommercial3rdPartyConfigurationWrites)

  implicit lazy val apiCommercialConfigurationFormat: Format[ApiCommercialConfiguration] =
    Format(apiCommercialConfigurationReads, apiCommercialConfigurationWrites)

  implicit lazy val apiThemeConfigurationFormat: Format[ApiThemeConfiguration] =
    Format(apiThemeConfigurationReads, apiThemeConfigurationWrites)

  implicit lazy val apiHeaderConfigurationFormat: Format[ApiHeaderConfiguration] =
    Format(apiHeaderConfigurationReads, apiHeaderConfigurationWrites)

  implicit lazy val apiBrandingConfigurationFormat: Format[ApiSponsoringConfiguration] =
    Format(apiBrandingConfigurationReads, apiBrandingConfigurationWrites)

  implicit lazy val apiMetaRobotsFormat: Format[ApiMetaRobots] =
    Format(apiMetaRobotsReads, apiMetaRobotsWrites)

  implicit lazy val apiMetaConfigurationFormat: Format[ApiMetaConfiguration] =
    Format(apiMetaConfigurationReads, apiMetaConfigurationWrites)

  implicit lazy val apiConfigurationFormat: Format[ApiConfiguration] =
    Format(apiConfigurationReads, apiConfigurationWrites)

  implicit lazy val apiPressedContentFormat: Format[ApiPressedContent] =
    Format(apiPressedContentReads, apiPressedContentWrites)

  implicit lazy val apiTeaserFormat: Format[ApiTeaser] =
    Format(apiTeaserReads, apiTeaserWrites)

  implicit lazy val apiStageFormat: Format[ApiStage] =
    Format(apiStageReads, apiStageWrites)

  implicit lazy val apiPressedSectionFormat: Format[ApiPressedSection] =
    Format(apiPressedSectionReads, apiPressedSectionWrites)

  implicit lazy val statusPhraseFormat: Format[StatusPhrase] =
    Format(statusPhraseReads, statusPhraseWrites)

  implicit lazy val apiPressedSectionResponseFormat: Format[ApiPressedSectionResponse] =
    Format(apiPressedSectionResponseReads, apiPressedSectionResponseWrites)
}
