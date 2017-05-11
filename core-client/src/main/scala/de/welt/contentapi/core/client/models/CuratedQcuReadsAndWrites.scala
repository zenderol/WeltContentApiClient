package de.welt.contentapi.core.client.models

import play.api.libs.json.{Json, Reads, Writes}

case class CuratedQcuReadsAndWrites(exportTime: String, ids: Seq[String])

object QcuResponseWrites {
  implicit val qcuTimestampedResponseWrites: Writes[CuratedQcuReadsAndWrites] = Json.writes[CuratedQcuReadsAndWrites]
}

object QcuResponseReads {
  implicit val qcuTimestampedResponseReads: Reads[CuratedQcuReadsAndWrites] = Json.reads[CuratedQcuReadsAndWrites]
}