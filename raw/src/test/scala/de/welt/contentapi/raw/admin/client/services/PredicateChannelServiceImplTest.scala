package de.welt.contentapi.raw.admin.client.services

import de.welt.contentapi.raw.client.services.RawTreeServiceImpl
import de.welt.contentapi.raw.models.{RawChannel, RawChannelStageConfiguredId, RawChannelStageCurated, RawChannelStageTracking}
import de.welt.testing.TestHelper
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.scalatestplus.play.PlaySpec

//noinspection ScalaStyle
class PredicateChannelServiceImplTest extends PlaySpec {
  val rts = Mockito.mock(classOf[RawTreeServiceImpl])
  val service: PredicateChannelServiceImpl = PredicateChannelServiceImpl(rts)

  trait CuratedFixture {
    private val stageConfig: RawChannelStageCurated = RawChannelStageCurated(
      index = 0,
      trackingName = None,
      link = None,
      curatedSectionMapping = "frontpage",
      curatedStageMapping = "politik",
      layout = None,
      label = None,
      logo = None,
      sponsoring = None
    )
    val config = TestHelper.raw.stageConfiguration.withStage(stageConfig)

    val child1 = TestHelper.raw.channel.emptyWithIdAndStageConfig(1, config)
    var child2 = TestHelper.raw.channel.emptyWithId(2)
    val child3 = TestHelper.raw.channel.emptyWithIdAndStageConfig(3, config)
    child2.children = Seq(child3)

    /**
      * (0)
      * /  \
      * (1) (2)
      * \
      * (3)
      */

    val root: RawChannel = TestHelper.raw.channel.emptyWithIdAndChildren(0, children = Seq(child1, child2))
    root.updateParentRelations()
    Mockito.when(rts.root).thenReturn(Some(root))
  }

  trait ConfiguredIdFixture {
    private val stageConfig: RawChannelStageConfiguredId = RawChannelStageConfiguredId(
      index = 0,
      trackingName = None,
      link = None,
      configuredId = "1234567890",
      label = None
    )
    val config = TestHelper.raw.stageConfiguration.withStage(stageConfig)

    val child1 = TestHelper.raw.channel.emptyWithIdAndStageConfig(1, config)
    var child2 = TestHelper.raw.channel.emptyWithId(2)
    val child3 = TestHelper.raw.channel.emptyWithIdAndStageConfig(3, config)
    child2.children = Seq(child3)

    /**
      * (0)
      * /  \
      * (1) (2)
      * \
      * (3)
      */

    val root: RawChannel = TestHelper.raw.channel.emptyWithIdAndChildren(0, children = Seq(child1, child2))
    root.updateParentRelations()
    Mockito.when(rts.root).thenReturn(Some(root))
  }

  trait WebtrekkFixture {
    private val stageConfig: RawChannelStageTracking = RawChannelStageTracking(
      index = 0,
      trackingName = None,
      link = None,
      layout = None,
      reportName = "report_1",
      label = None,
      logo = None
    )
    private val stageConfig2: RawChannelStageTracking = RawChannelStageTracking(
      index = 0,
      trackingName = None,
      link = None,
      layout = None,
      reportName = "report_foobar",
      label = None,
      logo = None
    )
    val config = TestHelper.raw.stageConfiguration.withStage(stageConfig)
    val config2 = TestHelper.raw.stageConfiguration.withStage(stageConfig2)

    val child1 = TestHelper.raw.channel.emptyWithIdAndStageConfig(1, config)
    var child2 = TestHelper.raw.channel.emptyWithIdAndStageConfig(2, config2)
    val child3 = TestHelper.raw.channel.emptyWithIdAndStageConfig(3, config)
    child2.children = Seq(child3)

    /**
      * (0)
      * /  \
      * (1) (2)
      * \
      * (3)
      */

    val root: RawChannel = TestHelper.raw.channel.emptyWithIdAndChildren(0, children = Seq(child1, child2))
    root.updateParentRelations()
    Mockito.when(rts.root).thenReturn(Some(root))
  }


  "PredicateSearchService" should {
    "find all children with same Papyrus config" in new CuratedFixture {
      private val channels: Seq[RawChannel] = service.findChannelsWithCuration("frontpage", "politik")
      channels.map(_.id.path) mustBe Seq("/1/", "/3/")
    }

    "find all children with same Configured ID" in new ConfiguredIdFixture {
      private val channels: Seq[RawChannel] = service.findChannelsWithConfiguredId("1234567890")
      channels.map(_.id.path) mustBe Seq("/1/", "/3/")
    }


    "find all children with same Webtrekk config" in new WebtrekkFixture {
      private val channels: Seq[RawChannel] = service.findChannelsWithWebtrekkReport("report_1")
      channels.map(_.id.path) mustBe Seq("/1/", "/3/")
    }

    "recovers with empty results if tree cant be accesses" in {
      Mockito.when(rts.root).thenReturn(None)
      service.findChannelsWithWebtrekkReport("report_1") mustBe Nil
    }

    "returns empty result set if None matches" in new ConfiguredIdFixture {
      service.findChannelsWithCuration("dick", "butt") mustBe Nil
    }
  }


}
