package de.welt.contentapi.raw.models.legacy

import de.welt.contentapi.raw.models.legacy.Datasource.{CuratedSource, SearchSource}
import de.welt.contentapi.raw.models.legacy.Query.{FlagQuery, SectionQuery, SubTypeQuery, TypeQuery}

case class Stage(id: String = "stageId",
                 index: Int = -1,
                 sources: Option[Seq[Datasource]] = None,
                 config: Option[ApiStageConfig] = None,
                 groups: Option[Seq[ApiStageGroup]] = None) {
  lazy val unwrappedSources: Seq[Datasource] = sources.getOrElse(Nil)
  lazy val unwrappedGroups: Seq[ApiStageGroup] = groups.getOrElse(Nil)
}

/**
  * rowType determines the Grid Layout
  * can be "stageHero" or "default"
  * e.g. StageHero with 2 Articles =>
  * ########|####
  * ########|####
  * ########|####
  * e.g. default with 2 Articles =>
  * ######|######
  * ######|######
  * ######|######
  */
case class ApiStageGroup(rowType: String = "default",
                         teaserType: String = "default")

case class ApiStageConfig(maxSize: Option[Int] = None,
                          stageTheme: Option[ApiStageTheme] = None,
                          headlineTheme: Option[ApiHeadlineTheme] = None,
                          stageType: Option[String] = None,
                          sectionReferences: Option[Seq[ApiSectionReference]] = None,
                          commercials: Option[Seq[ApiCommercial]] = None) {
  lazy val unwrappedSectionReferences: Seq[ApiSectionReference] = sectionReferences.getOrElse(Nil)
  lazy val unwrappedCommercials: Seq[ApiCommercial] = commercials.getOrElse(Nil)
}

case class ApiHeadlineTheme(label: String, small: Boolean = false)

case class ApiCommercial(format: String) {
  import ApiCommercialDefinitions._
  def apply(value: String): ApiCommercial = {
    value match {
      case MEDIUM_RECTANGLE.`format` => MEDIUM_RECTANGLE
      case BILLBOARD2.`format` => BILLBOARD2
      case _ => ApiCommercial(format = value)
    }
  }
}

object ApiCommercialDefinitions {
  lazy val MEDIUM_RECTANGLE = ApiCommercial(format = "MediumRectangle")
  lazy val BILLBOARD2 = ApiCommercial(format = "BillBoard2")
}

case class ApiSectionReference(path: String, label: String)

case class ApiStageTheme(name: String)

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



