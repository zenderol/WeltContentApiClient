package de.welt.contentapi.core.models

import de.welt.contentapi.core.models.Datasource.{CuratedSource, SearchSource}
import de.welt.contentapi.core.models.Query.{FlagQuery, SectionQuery, SubTypeQuery, TypeQuery}
import de.welt.contentapi.core.models.pressed.SectionReference

case class Stage(id: String = "default",
                 sources: Seq[Datasource],
                 config: StageConfig) {}

case class StageConfig(maxSize: Option[Int] = None,
                       path: Option[String] = None,
                       stageTheme: Option[StageTheme] = None,
                       headlineTheme: Option[HeadlineTheme] = None,
                       isHidden: Option[Boolean] = None,
                       typ: StageType = StageType("default"),
                       sectionReferences: Seq[SectionReference] = Nil,
                       commercial: Option[Commercial] = None)

case class Commercial(typ: String)

case class HeadlineTheme(headline: String,
                         size: Option[String],
                         weight: Option[String])

case class StageTheme(name: String = "default",
                      itemGap: Option[String] = None,
                      sectionGap: Option[String] = None,
                      bgColor: Option[String] = None,
                      frameless : Option[Boolean] = None)

case class StageType(typ: String)


object StageFormats {

  import play.api.libs.json._
  import pressed.PressedFormats.sectionReferenceFormat

  implicit lazy val headlineThemeFormat: Format[HeadlineTheme] = Json.format[HeadlineTheme]
  implicit lazy val commercialFormat: Format[Commercial] = Json.format[Commercial]
  implicit lazy val stageThemeFormat: Format[StageTheme] = Json.format[StageTheme]
  implicit lazy val stageConfigFormat: Format[StageConfig] = Json.format[StageConfig]
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


sealed trait Query {
  val filterNegative: Boolean
  val queryType: String

  override def toString = queryType
}

object Query {

  import play.api.libs.json._

  object QueryTypes {
    val typesQuery: String = "type"
    val subTypesQuery: String = "subType"
    val sectionsQuery: String = "section"
    val flagsQuery: String = "flags"
  }

  def unapply(query: Query): Option[(String, JsValue)] = {
    val (typ: String, sub) = query.queryType match {
      case QueryTypes.typesQuery =>
        val typedQuery = query.asInstanceOf[TypeQuery]
        (typedQuery.queryType, Json.toJson(typedQuery)(StageFormats.typeQueryFormat))
      case QueryTypes.subTypesQuery =>
        val typedQuery = query.asInstanceOf[SubTypeQuery]
        (typedQuery.queryType, Json.toJson(typedQuery)(StageFormats.subtypeQueryFormat))
      case QueryTypes.sectionsQuery =>
        val typedQuery = query.asInstanceOf[SectionQuery]
        (typedQuery.queryType, Json.toJson(typedQuery)(StageFormats.sectionQueryFormat))
      case QueryTypes.flagsQuery =>
        val typedQuery = query.asInstanceOf[FlagQuery]
        (typedQuery.queryType, Json.toJson(typedQuery)(StageFormats.flagQueryFormat))
    }
    Some(typ -> sub)
  }

  def apply(typ: String, query: JsValue): Query = {
    (typ match {
      case QueryTypes.typesQuery ⇒ Json.fromJson(query)(StageFormats.typeQueryFormat)
      case QueryTypes.subTypesQuery ⇒ Json.fromJson(query)(StageFormats.subtypeQueryFormat)
      case QueryTypes.sectionsQuery ⇒ Json.fromJson(query)(StageFormats.sectionQueryFormat)
      case QueryTypes.flagsQuery ⇒ Json.fromJson(query)(StageFormats.flagQueryFormat)
    }).get
  }

  case class TypeQuery(override val filterNegative: Boolean, queryValue: String)
    extends Query {
    override val queryType: String = QueryTypes.typesQuery
  }

  case class SubTypeQuery(override val filterNegative: Boolean, queryValue: String)
    extends Query {
    override val queryType: String = QueryTypes.subTypesQuery
  }

  case class SectionQuery(override val filterNegative: Boolean, queryValue: String)
    extends Query {
    override val queryType: String = QueryTypes.sectionsQuery
  }

  case class FlagQuery(override val filterNegative: Boolean, queryValues: Seq[String])
    extends Query {
    override val queryType: String = QueryTypes.flagsQuery
  }

}


sealed trait Datasource {

  def typ: String

  def maxSize: Option[Int]
}

object DatasourceTypes {
  val curatedSource = "curated"
  val searchSource = "search"
}

object Datasource {

  import play.api.libs.json._

  def unapply(datasource: Datasource): Option[(String, JsValue)] = {
    val (typ: String, sub) = datasource.typ match {
      case DatasourceTypes.curatedSource =>
        val typedSource = datasource.asInstanceOf[CuratedSource]
        (typedSource.typ, Json.toJson(typedSource)(StageFormats.curatedSourceFormat))
      case DatasourceTypes.searchSource =>
        val typedSource = datasource.asInstanceOf[SearchSource]
        (typedSource.typ, Json.toJson(typedSource)(StageFormats.searchSourceFormat))
    }
    Some(typ -> sub)
  }

  def apply(typ: String, sourceParams: JsValue): Datasource = {
    (typ match {
      case DatasourceTypes.curatedSource => Json.fromJson[CuratedSource](sourceParams)(StageFormats.curatedSourceFormat)
      case DatasourceTypes.searchSource => Json.fromJson[SearchSource](sourceParams)(StageFormats.searchSourceFormat)
    }).get
  }

  case class CuratedSource(override val maxSize: Option[Int],
                           papyrusFolder: String,
                           papyrusFile: String
                          ) extends Datasource {
    override val typ: String = DatasourceTypes.curatedSource
  }

  case class SearchSource(override val maxSize: Option[Int],
                          queries: Seq[Query] = Seq()) extends Datasource {
    override val typ: String = DatasourceTypes.searchSource
  }

}




