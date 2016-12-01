package de.welt.contentapi.core.client.models

case class ApiContentSearch(`type`: Option[MainTypeParam] = None,
                            subType: Option[SubTypeParam] = None,
                            section: Option[SectionParam] = None,
                            homeSection: Option[HomeSectionParam] = None,
                            sectionExcludes: Option[SectionExcludes] = None,
                            flags: Option[FlagParam] = None,
                            limit: Option[LimitParam] = None
                           ) {
  def allParams: Seq[Option[SearchParam]
    ] = Seq(`type`, subType, section, homeSection, sectionExcludes, flags, limit)

  def getAllParamsUnwrapped: Seq[(String, String)] = {
    allParams
      .filter(_.isDefined)
      .map(_.get)
      .map(_.getKeyValue)
  }
}

sealed trait SearchParam {
  val queryParamName: String
  val queryValue: String

  def getKeyValue = (queryParamName, queryValue)
}


case class MainTypeParam(queryValue: String)
  extends SearchParam {
  override val queryParamName: String = "type"
}

case class SubTypeParam(queryValue: String)
  extends SearchParam {
  override val queryParamName: String = "subType"
}

case class SectionParam(queryValue: String)
  extends SearchParam {
  override val queryParamName: String = "sectionPath"
}

case class HomeSectionParam(queryValue: String)
  extends SearchParam {
  override val queryParamName: String = "sectionHome"
}

case class SectionExcludes(queryValue: String)
  extends SearchParam {
  override val queryParamName: String = "excludeSections"
}

case class FlagParam(queryValue: String)
  extends SearchParam {
  override val queryParamName: String = "flag"
}

case class LimitParam(queryValue: String)
  extends SearchParam {
  override val queryParamName: String = "pageSize"
}


sealed trait Datasource {
  val `type`: String
  val maxSize: Option[Int]
}

case class CuratedSource(override val maxSize: Option[Int],
                         folderPath: String,
                         filename: String
                        ) extends Datasource {
  override val `type`: String = "curated"
}

case class SearchSource(override val maxSize: Option[Int],
                        queries: Seq[SearchParam] = Seq()) extends Datasource {
  override val `type`: String = "search"
}



