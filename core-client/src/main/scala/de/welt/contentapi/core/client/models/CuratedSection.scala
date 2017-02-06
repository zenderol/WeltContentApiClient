package de.welt.contentapi.core.client.models

/**
  * Section Representation in Papyrus
  * The Papyrus Api REST Endpoint only delivers Seq[CuratedStage]
  * these are consolidated into the CuratedSection object
  *
  * @param stages list of curated stages
  * @param name REST path part to access the curation e.g. 'icon'
  */
case class CuratedSection(stages: Seq[CuratedStage], name: Option[String] = None) {
  def allCuratedStageIds = stages.map(_.id)
}

/**
  * Stage Representation in Papyrus
  * delivered by the Curation Api REST Endpoint
  *
  * @param id identifier of the stage, used for mapping to Janus 1 or 2 Layout Configuration
  * @param articles content that is curated in this stage
  * @param title this title is meant for presentation in Funkotron on a stage
  */
case class CuratedStage(id: String, articles: Seq[CuratedItem] = Nil, title: Option[String] = None) {
  def escenicIdsForStage: Seq[String] = articles.map(_.id)
}

/**
  * A single curated item, only field is its escenicId
  *
  * @param id is used to resolve the Curated Item at the ContentApi
  */
case class CuratedItem(id: String)

