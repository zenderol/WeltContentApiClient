package de.welt.contentapi.core.models

import de.welt.contentapi.core.models.Datasource.{CuratedSource, SearchSource}
import de.welt.contentapi.core.models.Query.{FlagQuery, SectionQuery, SubTypeQuery, TypeQuery}

case class Stage(id: String = "stage",
                 index: Int = -1,
                 sources: Seq[Datasource] = Nil,
                 config: StageConfig = StageConfig())

case class StageConfig(maxSize: Option[Int] = None,
                       stageTheme: StageTheme = StageTheme(),
                       headlineTheme: Option[HeadlineTheme] = None,
                       stageType: StageType = StageType(),
                       sectionReferences: Seq[SectionReference] = Nil,
                       commercial: Option[Commercial] = None)


case class StageTheme(stageLayout: StageLayout = StageLayout(),
                      stageBgColor: StageBgColor = StageBgColor.DEFAULT)

case class StageType(typ: String = "default",
                     lazyloaded: Boolean = false,
                     hidden: Boolean = false)

object StageType {
  val USER_DEFINED = StageType(typ = "user-defined", lazyloaded = true)
  val TEMPORARY_HIDDEN = StageType(hidden = true)
}

case class HeadlineTheme(text: String,
                         small: Boolean = false)

case class Commercial(name: String)

object Commercial {
  val MEDIUM_RECTANGLE = Commercial("MediumRectangle")
  val BILLBOARD2 = Commercial("BillBoard2")
}

case class SectionReference(path: String, label: String)

case class StageLayout(name: String = "default")

object StageLayout {

  lazy val HERO = StageLayout(name = "1-hero-3-small-2-wide") // hero
  lazy val SIMPLE_GRID = StageLayout(name = "x-small") // default
  lazy val MULTIMEDIA_BIG = StageLayout(name = "big-img_1-3-2") // MediaHero
  lazy val TWO_COLUMN_ORDERED_LIST = StageLayout(name = "2-column_orderedList") // Meistgelesen
  lazy val ONE_MEDIUM_GRID = StageLayout(name = "1-medium_x-small") // Sport
  lazy val WIDE_HERO_PLUS_INLINE_THIRDS = StageLayout(name = "1-wide_3-inline-thirds") // Sport
  lazy val WIDE_HERO_PLUS_INLINE_HALFS = StageLayout(name = "1-wide_3-inline-halfs") // Sport

  // ... more Layouts
}

case class StageBgColor(color: String)

object StageBgColor {
  lazy val DEFAULT = StageBgColor("")

  lazy val BLARZ = StageBgColor("blarz")
  lazy val DARK = StageBgColor("dark")
  lazy val LIGHT = StageBgColor("light")
}


object StageFormats {

  import play.api.libs.json._

  // need typesafe val, because default Type is OFormat[...]
  implicit lazy val stageBgColorFormat: Format[StageBgColor] = Json.format[StageBgColor]
  implicit lazy val commercialFormat: Format[Commercial] = Json.format[Commercial]
  implicit lazy val sectionReferenceFormat: Format[SectionReference] = Json.format[SectionReference]
  implicit lazy val headlineThemeFormat: Format[HeadlineTheme] = Json.format[HeadlineTheme]
  implicit lazy val stageThemeFormat: Format[StageTheme] = Json.format[StageTheme]
  implicit lazy val stageLayoutFormat: Format[StageLayout] = Json.format[StageLayout]
  implicit lazy val stageTypeFormat: Format[StageType] = Json.format[StageType]
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




