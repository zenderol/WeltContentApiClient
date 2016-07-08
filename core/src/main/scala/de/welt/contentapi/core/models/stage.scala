package de.welt.contentapi.core.models

import play.api.libs.json.{Json, JsValue}

case class Stage(channelId: ChannelId,
                 maxSize: Option[Int] = None,
                 sources: Seq[Datasource],
                 layout: Option[String] = None, // fixed, dynamic todo
                 lazyLoaded: Boolean = false,
                 headline: Option[String] = None,
                 path: Option[String] = None) {
}

sealed trait Query {
  val filterNegative: Boolean
  val queryType: String
  override def toString = queryType
}
object Query {
  object QueryTypes {
    val typesQuery: String = "type"
    val subTypesQuery: String = "subType"
    val sectionsQuery: String = "section"
    val flagsQuery: String = "flags"
  }

  def unapply(query: Query): Option[(String, JsValue)] = {
    val (typ: String, sub) = query.queryType match {
      case QueryTypes.typesQuery => {
        val typedQuery = query.asInstanceOf[TypeQuery]
        (typedQuery.queryType, Json.toJson(typedQuery)(SimpleFormats.typeQueryFormat))
      }
      case QueryTypes.subTypesQuery => {
        val typedQuery = query.asInstanceOf[SubTypeQuery]
        (typedQuery.queryType, Json.toJson(typedQuery)(SimpleFormats.subtypeQueryFormat))
      }
      case QueryTypes.sectionsQuery => {
        val typedQuery = query.asInstanceOf[SectionQuery]
        (typedQuery.queryType, Json.toJson(typedQuery)(SimpleFormats.sectionQueryFormat))
      }
      case QueryTypes.flagsQuery => {
        val typedQuery = query.asInstanceOf[FlagQuery]
        (typedQuery.queryType, Json.toJson(typedQuery)(SimpleFormats.flagQueryFormat))
      }
    }
    Some(typ -> sub)
  }

  def apply(typ: String, query: JsValue): Query = {
    (typ match {
      case QueryTypes.sectionsQuery => Json.fromJson[TypeQuery](query)(SimpleFormats.typeQueryFormat)
      case QueryTypes.subTypesQuery => Json.fromJson[SubTypeQuery](query)(SimpleFormats.subtypeQueryFormat)
      case QueryTypes.sectionsQuery => Json.fromJson[SectionQuery](query)(SimpleFormats.sectionQueryFormat)
      case QueryTypes.flagsQuery => Json.fromJson[FlagQuery](query)(SimpleFormats.flagQueryFormat)
    }).get
  }

  case class TypeQuery(override val filterNegative: Boolean, val queryValue: String)
  extends Query {
    override val queryType: String = QueryTypes.typesQuery
  }

  case class SubTypeQuery(override val filterNegative: Boolean, val queryValue: String)
    extends Query {
    override val queryType: String = QueryTypes.subTypesQuery
  }

  case class SectionQuery(override val filterNegative: Boolean, val queryValue: String)
    extends Query {
    override val queryType: String = QueryTypes.sectionsQuery
  }

  case class FlagQuery(override val filterNegative: Boolean, val queryValues: Seq[String])
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
  def unapply(datasource: Datasource): Option[(String, JsValue)] = {
    val (typ: String, sub) = datasource.typ match {
      case DatasourceTypes.curatedSource => {
        val typedSource = datasource.asInstanceOf[CuratedSource]
        (typedSource.typ, Json.toJson(typedSource)(SimpleFormats.curatedSourceFormat))
      }
      case DatasourceTypes.searchSource => {
        val typedSource = datasource.asInstanceOf[SearchSource]
        (typedSource.typ, Json.toJson(typedSource)(SimpleFormats.searchSourceFormat))
      }
    }
    Some(typ -> sub)
  }

  def apply(typ: String, sourceParams: JsValue): Datasource = {
    (typ match {
      case DatasourceTypes.curatedSource => Json.fromJson[CuratedSource](sourceParams)(SimpleFormats.curatedSourceFormat)
      case DatasourceTypes.searchSource => Json.fromJson[SearchSource](sourceParams)(SimpleFormats.searchSourceFormat)
    }).get
  }

  case class CuratedSource(override val maxSize: Option[Int],
                           val papyrusFolder: String,
                           val papyrusFile: String
                          ) extends Datasource {
    override val typ: String = DatasourceTypes.curatedSource
  }

  case class SearchSource(override val maxSize: Option[Int],
                          val queries: Seq[Query] = Seq()) extends Datasource {
    override val typ: String = DatasourceTypes.searchSource
  }
}




