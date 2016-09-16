package de.welt.contentapi.core.models


import com.google.common.collect.ImmutableMap
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


object StageTypeDefinitions {
  lazy val USER_DEFINED = StageType(typ = "user-defined", lazyloaded = true)
  lazy val HIDDEN = StageType(typ = "default", hidden = true)
  lazy val DEFAULT = StageType(typ = "default")

}
case class StageType(typ: String = "default", lazyloaded: Boolean = false, hidden: Boolean = false) {
  import StageTypeDefinitions._
  private lazy val constants: ImmutableMap[String, StageType] = ImmutableMap
    .builder()
      .put(USER_DEFINED.typ, USER_DEFINED)
      .put(HIDDEN.typ, HIDDEN)
    .build()
  def apply(typ: String = "default"): StageType = {
    constants.getOrDefault(typ, DEFAULT)
  }
}


case class HeadlineTheme(text: String, small: Boolean = false)

case class Commercial(name: String) {
  import CommercialDefinitions._
  def apply(value: String): Commercial = {
    value match {
      case MEDIUM_RECTANGLE.name => MEDIUM_RECTANGLE
      case BILLBOARD2.name => BILLBOARD2
      case _ => Commercial(name = value)
    }
  }
}

object CommercialDefinitions {
  lazy val MEDIUM_RECTANGLE = Commercial(name = "MediumRectangle")
  lazy val BILLBOARD2 = Commercial(name = "BillBoard2")
}

case class SectionReference(path: String, label: String)


case class StageTheme(stageLayout: StageLayout = StageLayout(),
                      stageBgColor: StageColor = StageColor(""))


object StageLayoutDefinitions {
  lazy val DEFAULT = StageLayout(name = "x-smalls") // Default
  lazy val HERO = StageLayout("1-hero-3-small-2-wide")
  lazy val MEDIA_HERO = StageLayout("big-img_1-3-2")
  lazy val MEISTGELESEN = StageLayout("2-column_orderedList", itemGap = Some("small"), sectionGap = Some("vertical"))
  lazy val HERO_MEDIUM = StageLayout("1-medium_x-small")
  lazy val HERO_X_BIG_THIRDS = StageLayout("1-wide_x-big-thirds")
  lazy val MEDIATHEK = StageLayout("1-wide_x-squares")
  lazy val HALFS_ONLY = StageLayout("x-halfs")
  lazy val HIGHLIGHTS= StageLayout("1-wide_3-big-thirds-x-big-halfs")
}

case class StageLayout(name: String = "default", itemGap: Option[String] = None, sectionGap: Option[String] = None) {
  import StageLayoutDefinitions._
  private lazy val constants: ImmutableMap[String, StageLayout] = ImmutableMap
    .builder()
    .put(HERO.name, HERO)
    .put(MEDIA_HERO.name, MEDIA_HERO)
    .put(MEISTGELESEN.name, MEISTGELESEN) // Meistgelesen
    .put(HERO_MEDIUM.name, HERO_MEDIUM) // e.g. Sport
    .put(HERO_X_BIG_THIRDS.name, HERO_X_BIG_THIRDS) // e.g. Schönes Leben
    .put(MEDIATHEK.name, MEDIATHEK) // e.g. Mediathek on Home
    .put(HALFS_ONLY.name, HALFS_ONLY) // e.g. Bilder des Tages
    .put(HIGHLIGHTS.name, HIGHLIGHTS) // e.g. Highlights
    .build()

  def apply(name: String): StageLayout = {
    constants.getOrDefault(name, DEFAULT)
  }
}


object StageColorDefinitions {
  lazy val BLARZ = StageColor(bgColor = "blarz")
  lazy val DARK = StageColor(bgColor = "dark")
  lazy val LIGHT = StageColor(bgColor = "light")
  lazy val DEFAULT = StageColor(bgColor = "", invertedTextColor = false)
}

case class StageColor(bgColor: String, invertedTextColor: Boolean = true) {
  import StageColorDefinitions._
  private lazy val constants: ImmutableMap[String, StageColor] = ImmutableMap
    .builder()
    .put(BLARZ.bgColor, BLARZ)
    .put(DARK.bgColor, DARK)
    .put(LIGHT.bgColor, LIGHT)
    .build()
  def apply(bgColor: String): StageColor = {
    constants.getOrDefault(bgColor, DEFAULT)
  }
}


object StageFormats {

  import play.api.libs.json._

  // need typesafe val, because default Type is OFormat[...]
  implicit lazy val stageBgColorFormat: Format[StageColor] = Json.format[StageColor]
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




