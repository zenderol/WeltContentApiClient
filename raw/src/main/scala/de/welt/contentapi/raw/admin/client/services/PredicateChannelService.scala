package de.welt.contentapi.raw.admin.client.services

import java.util.function.Predicate

import com.google.inject.ImplementedBy
import de.welt.contentapi.raw.client.services.RawTreeService
import de.welt.contentapi.raw.models._
import de.welt.contentapi.utils.Loggable
import javax.inject.Inject

@ImplementedBy(classOf[PredicateChannelServiceImpl])
trait PredicateChannelService {
  def findChannelsWithWebtrekkReport(report: String): Seq[RawChannel]
  def findChannelsWithCuration(section: String, stage: String): Seq[RawChannel]
  def findChannelsWithConfiguredId(id: String): Seq[RawChannel]
}

case class PredicateChannelServiceImpl @Inject()(rts: RawTreeService) extends PredicateChannelService with Loggable {

  def findChannelsWithWebtrekkReport(report: String): Seq[RawChannel] = findByPredicate(hasWebtrekkStage(report))

  def findChannelsWithCuration(section: String, stage: String): Seq[RawChannel] = findByPredicate(hasCuratedStageWithValues(section, stage))

  def findChannelsWithConfiguredId(id: String): Seq[RawChannel] = findByPredicate(hasConfiguredId(id))

  private def findByPredicate(p: Predicate[RawChannel]): Seq[RawChannel] = rts.root
    .map(_.findByPredicate(p))
    .getOrElse {
      log.error("The Section Tree could not be loaded. Trying to recover by returning empty result sets.")
      Nil
    }

  private def hasCuratedStageWithValues(sec: String, stg: String): Predicate[RawChannel] = _.stageConfiguration
    .flatMap { stageConfig: RawChannelStageConfiguration =>
      stageConfig.stages
        .getOrElse(Nil)
        .find(s => s.hasType(RawChannelStage.TypeCurated) && s.asInstanceOf[RawChannelStageCurated].hasValues(sec, stg))
    }.nonEmpty


  private def hasConfiguredId(id: String): Predicate[RawChannel] = _.stageConfiguration
    .flatMap { stageConfig: RawChannelStageConfiguration =>
      stageConfig.stages
        .getOrElse(Nil)
        .find(s => s.hasType(RawChannelStage.TypeConfiguredId) && s.asInstanceOf[RawChannelStageConfiguredId].configuredId == id)
    }.nonEmpty


  private def hasWebtrekkStage(report: String): Predicate[RawChannel] = _.stageConfiguration
    .flatMap { stageConfig: RawChannelStageConfiguration =>
      stageConfig
        .stages.getOrElse(Nil)
        .find(s => s.hasType(RawChannelStage.TypeTracking) && s.asInstanceOf[RawChannelStageTracking].reportName == report)
    }.nonEmpty
}
