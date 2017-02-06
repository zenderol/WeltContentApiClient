package de.welt.contentapi.core.client.models

import play.api.libs.json.{Json, Reads}

object CuratedReads {
  implicit val curatedItemRead: Reads[CuratedItem] = Json.reads[CuratedItem]
  implicit val curatedItemSeqReads: Reads[Seq[CuratedItem]] = Reads.seq(curatedItemRead)
  implicit val curatedStageReads: Reads[CuratedStage] = Json.reads[CuratedStage]
  implicit val curatedStagesSeqReads: Reads[Seq[CuratedStage]] = Reads.seq(curatedStageReads)
}
