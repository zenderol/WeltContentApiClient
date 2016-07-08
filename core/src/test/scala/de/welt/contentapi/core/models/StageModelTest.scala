package de.welt.contentapi.core.models

import java.nio.charset.Charset
import java.time.Instant

import de.welt.contentapi.core.models.Datasource._
import de.welt.contentapi.core.models.Query._
import org.apache.commons.io.FileUtils
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsValue, Json}

class StageModelTest extends PlaySpec {

  "The Config Model for Stages" must {

    "produces a curated Stage " in {

      val limitStageSize = 12
      val limitCurationSize = 12
      val eceId: Long = 1002L
      val id: ChannelId = ChannelId("/politik/ausland/", isVirtual = false, ece = eceId)

      val papyrusAuslandSource: CuratedSource = CuratedSource(
        Some(limitStageSize),
        papyrusFolder = "home",
        papyrusFile = "ausland")

      val curatedStage: Stage = Stage(
        channelId = id,
        sources = Seq(papyrusAuslandSource),
        lazyLoaded = false,
        maxSize = Some(limitCurationSize),
        headline = Some("stage-headline"),
        path = Some("/stage-link/"),
        layout = None)

      val politicsChannel: Channel = Channel(
        id = curatedStage.channelId,
        data = ChannelData("channel-label", ChannelAdData(false), ChannelMetadata()),
        stages = Seq(curatedStage),
        parent = None,
        children = Seq.empty,
        hasChildren = false,
        lastModifiedDate = Instant.now.toEpochMilli)

      import de.welt.contentapi.core.models.writes._
      implicit val writes = FullChannelWrites.channelWrites

      val json: JsValue = Json.toJson(politicsChannel)
      FileUtils.writeByteArrayToFile(new java.io.File("/tmp/curatedStage.json"), json.toString().getBytes(Charset.defaultCharset()))

      politicsChannel.stages.size mustBe 1

    }

    "produces a SearchApi Stage " in {
      val typeQuery: TypeQuery = TypeQuery(filterNegative = false, queryValue = "article")
      val subTypeQuery: SubTypeQuery = SubTypeQuery(filterNegative = true, queryValue = "broadcast")
      val sectionQuery: SectionQuery = SectionQuery(filterNegative = false, queryValue = "/politik/")
      val flagQuery: FlagQuery = FlagQuery(filterNegative = false, queryValues = Seq("highlight"))

      val searchLimit = 6
      val stageLimit = 12
      val allSearchQueriesDataSource: SearchSource = SearchSource(
        maxSize = Some(searchLimit),
        queries = Seq(typeQuery, subTypeQuery, sectionQuery, flagQuery))

      val searchStage: Stage = Stage(
        channelId = ChannelId("/politik/"),
        lazyLoaded = true,
        maxSize = Some(stageLimit),
        headline = Some("headline"),
        path = Some("/stage-link/"),
        layout = None,
        sources = Seq(allSearchQueriesDataSource)
      )

      val politikChannel: Channel = Channel(
        id = searchStage.channelId,
        data = ChannelData("channel-label", ChannelAdData(false), ChannelMetadata()),
        stages = Seq(searchStage),
        parent = None,
        children = Seq.empty,
        hasChildren = false,
        lastModifiedDate = Instant.now.toEpochMilli)




      import de.welt.contentapi.core.models.writes._
      implicit val writes = FullChannelWrites.channelWrites

      val json: JsValue = Json.toJson(politikChannel)
      FileUtils.writeByteArrayToFile(new java.io.File("/tmp/searchStage.json"), json.toString().getBytes(Charset.defaultCharset()))

      politikChannel.stages.size mustBe 1

    }

  }


  "The Config Model for SearchApiFilters" must {


    "produce a TypeFilter with a `type query`" in {
      val filterValue: String = "article"
      val filter: TypeQuery = TypeQuery(filterNegative = false, queryValue = filterValue)

      val filters: Seq[Query] = Seq(filter)
      val apiSourceWithFilter: SearchSource = SearchSource(
        maxSize = Some(24), queries = filters)
      val apiHighlightStage: Stage = Stage(
        maxSize = Some(24), sources = Seq(apiSourceWithFilter), layout = None, lazyLoaded = false, headline = Some("Politik"), path = Some("/politik/"), channelId = ChannelId("/home/"))

      val source: SearchSource = apiHighlightStage.sources.head.asInstanceOf[SearchSource]

      source.queries.head.getClass mustBe classOf[TypeQuery]
    }

    "produce a SubTypeFilter with a `subType query`" in {
      val filterValue: String = "news"
      val filter: SubTypeQuery = SubTypeQuery(filterNegative = false, queryValue = filterValue)

      val filters: Seq[Query] = Seq(filter)
      val apiSourceWithFilter: SearchSource = SearchSource(
        maxSize = Some(24), queries = filters)
      val apiHighlightStage: Stage = Stage(
        maxSize = Some(24), sources = Seq(apiSourceWithFilter), layout = None, lazyLoaded = false, headline = Some("Politik"), path = Some("/politik/"), channelId = ChannelId("/home/"))

      val source: SearchSource = apiHighlightStage.sources.head.asInstanceOf[SearchSource]
      source.queries.head.getClass mustBe classOf[SubTypeQuery]
    }

    "produce a SectionFilter with a `sectionPath query`" in {
      val filterValue: String = "/testgpr/"
      val filter: SectionQuery = SectionQuery(filterNegative = false, queryValue = filterValue)

      val filters: Seq[Query] = Seq(filter)
      val apiSourceWithFilter: SearchSource = SearchSource(
        maxSize = Some(24), queries = filters)
      val apiHighlightStage: Stage = Stage(
        maxSize = Some(24), sources = Seq(apiSourceWithFilter), layout = None, lazyLoaded = false, headline = Some("Politik"), path = Some("/politik/"), channelId = ChannelId("/home/"))

      val source: SearchSource = apiHighlightStage.sources.head.asInstanceOf[SearchSource]
      source.queries.head.getClass mustBe classOf[SectionQuery]
    }

    "produce a FlagFilter with a `flags query`" in {
      val filterValue: String = "PREMIUM"
      val filter: FlagQuery = FlagQuery(filterNegative = false, queryValues = Seq(filterValue))

      val filters: Seq[Query] = Seq(filter)
      val apiSourceWithFilter: SearchSource = SearchSource(
        maxSize = Some(24), queries = filters)
      val apiHighlightStage: Stage = Stage(
        maxSize = Some(24), sources = Seq(apiSourceWithFilter), layout = None, lazyLoaded = false, headline = Some("Politik"), path = Some("/politik/"), channelId = ChannelId("/home/"))

      val source: SearchSource = apiHighlightStage.sources.head.asInstanceOf[SearchSource]
      source.queries.head.getClass mustBe classOf[FlagQuery]
    }

  }
  "The Config Model for CuratedSources" must {
    "produce a PapyrusSource with `bucket` and `filename`" in {

      val folder: String = "homepage"
      val file: String = "aufmacher"
      val papyrusPolitikSource: CuratedSource = CuratedSource(
        maxSize = Some(24), papyrusFolder = folder, papyrusFile = file)
      val papyrusPolitikStage: Stage = Stage(
        maxSize = Some(24), sources = Seq(papyrusPolitikSource), layout = None, lazyLoaded = false, headline = Some("Politik"), path = Some("/politik/"), channelId = ChannelId("/home/"))

      papyrusPolitikStage.sources.head.asInstanceOf[CuratedSource].papyrusFile mustBe file
      papyrusPolitikStage.sources.head.asInstanceOf[CuratedSource].papyrusFolder mustBe folder

    }

  }

}
