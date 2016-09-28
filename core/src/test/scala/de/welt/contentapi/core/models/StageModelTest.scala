package de.welt.contentapi.core.models

import java.nio.charset.Charset
import java.time.Instant

import de.welt.contentapi.core.models.Datasource._
import de.welt.contentapi.core.models.Query.{QueryTypes, _}
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
        stageType = Some("")
        )

      val curatedStage: Stage = Stage(
        index = 0,
        sources = Some(Seq(papyrusAuslandSource)),
        config = Some(config))

      val politicsChannel: ApiChannel = ApiChannel(
        id = id,
        data = ApiChannelData("channel-label", ApiChannelAdData(false), ApiChannelMetadata()),
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
        stageType = Some(""))
      val searchStage: Stage = Stage(
        index = 0,
        sources = Some(Seq(allSearchQueriesDataSource)),
        config = Some(config)
      )


      val politikChannel: ApiChannel = ApiChannel(
        id = ChannelId("/politik/"),
        data = ApiChannelData("channel-label", ApiChannelAdData(false), ApiChannelMetadata()),
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
        stageType = Some(""))

      val apiHighlightStage: Stage = Stage(
        index = 0,
        sources = Some(Seq(apiSourceWithFilter)),
        config = Some(config))

      apiHighlightStage.sources.foreach(_.head.asInstanceOf[SearchSource].queries.head.queryType mustBe QueryTypes.typesQuery)
    }

    "produce a SubTypeFilter with a `subType query`" in {
      val filterValue: String = "news"
      val filter: SubTypeQuery = SubTypeQuery(filterNegative = false, queryValue = filterValue)

      val filters: Seq[Query] = Seq(filter)
      val apiSourceWithFilter: SearchSource = SearchSource(
        maxSize = Some(defaultMaxSize), queries = filters)

      val config: StageConfig = StageConfig(
        maxSize = Some(defaultMaxSize),
        stageType = Some(""))
      val apiHighlightStage: Stage = Stage(
        index = 0,
        sources = Some(Seq(apiSourceWithFilter)),
        config = Some(config)
      )

      apiHighlightStage.sources.foreach(_.head.asInstanceOf[SearchSource].queries.head.queryType mustBe QueryTypes.subTypesQuery)
    }

    "produce a SectionFilter with a `sectionPath query`" in {
      val filterValue: String = "/testgpr/"
      val filter: SectionQuery = SectionQuery(filterNegative = false, queryValue = filterValue)

      val filters: Seq[Query] = Seq(filter)
      val apiSourceWithFilter: SearchSource = SearchSource(
        maxSize = Some(defaultMaxSize), queries = filters)
      val config: StageConfig = StageConfig(
        maxSize = Some(defaultMaxSize),
        stageType = Some(""))
      val apiHighlightStage: Stage = Stage(
        index = 0,
        sources = Some(Seq(apiSourceWithFilter)),
        config = Some(config)
      )

      apiHighlightStage.sources.foreach(_.head.asInstanceOf[SearchSource].queries.head.queryType mustBe QueryTypes.sectionsQuery)
    }

    "produce a FlagFilter with a `flags query`" in {
      val filterValue: String = "PREMIUM"
      val filter: FlagQuery = FlagQuery(filterNegative = false, queryValues = Seq(filterValue))

      val filters: Seq[Query] = Seq(filter)
      val apiSourceWithFilter: SearchSource = SearchSource(
        maxSize = Some(defaultMaxSize), queries = filters)

      val config: StageConfig = StageConfig(
        maxSize = Some(defaultMaxSize),
        stageType = Some("default"))
      val apiHighlightStage: Stage = Stage(
        index = 0,
        sources = Some(Seq(apiSourceWithFilter)),
        config = Some(config)
      )

      apiHighlightStage.sources.foreach(_.head.asInstanceOf[SearchSource].queries.head.queryType mustBe QueryTypes.flagsQuery)

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
        stageType = Some("default"))
      val papyrusPolitikStage: Stage = Stage(
        index = 0,
        sources = Some(Seq(papyrusPolitikSource)),
        config = Some(config)
      )
      papyrusPolitikStage.sources.foreach(_.head.asInstanceOf[CuratedSource].papyrusFile mustBe file )
    }

  }

}
