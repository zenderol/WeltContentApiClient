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


      val config: StageConfig = StageConfig(
        maxSize = Some(limitCurationSize),
        lazyLoaded = Some(false),
        path = Some("/stage-link/"))

      val curatedStage: Stage = Stage(
        sources = Seq(papyrusAuslandSource),
        config = config)

      val politicsChannel: Channel = Channel(
        id = id,
        data = ChannelData("channel-label", ChannelAdData(false), ChannelMetadata()),
        stages = Some(Seq(curatedStage)),
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

      val config: StageConfig = StageConfig(
        maxSize = Some(stageLimit),
        lazyLoaded = Some(false),
        path = Some("/stage-link/"))
      val searchStage: Stage = Stage(
        sources = Seq(allSearchQueriesDataSource),
        config = config
      )


      val politikChannel: Channel = Channel(
        id = ChannelId("/politik/"),
        data = ChannelData("channel-label", ChannelAdData(false), ChannelMetadata()),
        stages = Some(Seq(searchStage)),
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


  private val defaultMaxSize: Int = 24
  "The Config Model for SearchApiFilters" must {


    "produce a TypeFilter with a `type query`" in {
      val filterValue: String = "article"
      val filter: TypeQuery = TypeQuery(filterNegative = false, queryValue = filterValue)

      val filters: Seq[Query] = Seq(filter)
      val apiSourceWithFilter: SearchSource = SearchSource(
        maxSize = Some(defaultMaxSize), queries = filters)

      val config: StageConfig = StageConfig(
        maxSize = Some(defaultMaxSize),
        lazyLoaded = Some(false),
        path = Some("/stage-link/"))

      val apiHighlightStage: Stage = Stage(
        sources = Seq(apiSourceWithFilter),
        config = config)

      val source: SearchSource = apiHighlightStage.sources.head.asInstanceOf[SearchSource]

      source.queries.head.getClass mustBe classOf[TypeQuery]
    }

    "produce a SubTypeFilter with a `subType query`" in {
      val filterValue: String = "news"
      val filter: SubTypeQuery = SubTypeQuery(filterNegative = false, queryValue = filterValue)

      val filters: Seq[Query] = Seq(filter)
      val apiSourceWithFilter: SearchSource = SearchSource(
        maxSize = Some(defaultMaxSize), queries = filters)

      val config: StageConfig = StageConfig(
        maxSize = Some(defaultMaxSize),
        lazyLoaded = Some(false),
        path = Some("/stage-link/"))
      val apiHighlightStage: Stage = Stage(
        sources = Seq(apiSourceWithFilter),
        config = config
      )

      val source: SearchSource = apiHighlightStage.sources.head.asInstanceOf[SearchSource]
      source.queries.head.getClass mustBe classOf[SubTypeQuery]
    }

    "produce a SectionFilter with a `sectionPath query`" in {
      val filterValue: String = "/testgpr/"
      val filter: SectionQuery = SectionQuery(filterNegative = false, queryValue = filterValue)

      val filters: Seq[Query] = Seq(filter)
      val apiSourceWithFilter: SearchSource = SearchSource(
        maxSize = Some(defaultMaxSize), queries = filters)
      val config: StageConfig = StageConfig(
        maxSize = Some(defaultMaxSize),
        lazyLoaded = Some(false),
        path = Some("/stage-link/"))
      val apiHighlightStage: Stage = Stage(
        sources = Seq(apiSourceWithFilter),
        config = config
      )

      val source: SearchSource = apiHighlightStage.sources.head.asInstanceOf[SearchSource]
      source.queries.head.getClass mustBe classOf[SectionQuery]
    }

    "produce a FlagFilter with a `flags query`" in {
      val filterValue: String = "PREMIUM"
      val filter: FlagQuery = FlagQuery(filterNegative = false, queryValues = Seq(filterValue))

      val filters: Seq[Query] = Seq(filter)
      val apiSourceWithFilter: SearchSource = SearchSource(
        maxSize = Some(defaultMaxSize), queries = filters)

      val config: StageConfig = StageConfig(
        maxSize = Some(defaultMaxSize),
        lazyLoaded = Some(false),
        path = Some("/stage-link/"))
      val apiHighlightStage: Stage = Stage(
        sources = Seq(apiSourceWithFilter),
        config = config
      )

      val source: SearchSource = apiHighlightStage.sources.head.asInstanceOf[SearchSource]
      source.queries.head.getClass mustBe classOf[FlagQuery]
      implicit val query = Json.format[Query]
      implicit val sourceW = Json.writes[SearchSource]
    }

  }
  "The Config Model for CuratedSources" must {
    "produce a PapyrusSource with `bucket` and `filename`" in {

      val folder: String = "homepage"
      val file: String = "aufmacher"
      val papyrusPolitikSource: CuratedSource = CuratedSource(
        maxSize = Some(defaultMaxSize), papyrusFolder = folder, papyrusFile = file)
      val config: StageConfig = StageConfig(
        maxSize = Some(defaultMaxSize),
        lazyLoaded = Some(false),
        path = Some("/stage-link/"))
      val papyrusPolitikStage: Stage = Stage(
        sources = Seq(papyrusPolitikSource),
        config = config
      )

      papyrusPolitikStage.sources.head.asInstanceOf[CuratedSource].papyrusFile mustBe file
      papyrusPolitikStage.sources.head.asInstanceOf[CuratedSource].papyrusFolder mustBe folder

    }

  }

}
