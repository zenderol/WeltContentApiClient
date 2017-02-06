package de.welt.contentapi.core.client.services.curation

import de.welt.contentapi.core.client.models.{CuratedItem, CuratedSection, CuratedStage}
import de.welt.contentapi.core.client.repository.PapyrusRepository
import de.welt.contentapi.core.client.services.exceptions.HttpClientErrorException
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

class CurationServiceImplTest extends PlaySpec with MockitoSugar {

  trait TestScope {
    val mockPapyrusRepo = mock[PapyrusRepository]
    val curationService = new CurationServiceImpl(mockPapyrusRepo)
  }

  "CurationService" must {
    "return None if CuratedSection does not exists (HttpClientError)" in new TestScope {
      // Given
      when(mockPapyrusRepo.getByName("test")(Nil, ec = ExecutionContext.global)).thenThrow(HttpClientErrorException(404, "My Fancy 404 Status Phrase", "/foo/"))

      // When
      val result = Await.result(curationService.curatedSectionForName("test")(Nil, ec = ExecutionContext.global), 1.second)

      // Then
      result mustBe None
    }

    "return Some(CuratedSection) if section exists" in new TestScope {
      // Given
      val section = CuratedSection(
        stages = Seq(
          CuratedStage(
            id = "Stage ID",
            articles = Seq(
              CuratedItem(
                id = "123456789"
              )
            ),
            title = Some("Stage Title")
          )
        )
      )
      when(mockPapyrusRepo.getByName("test")(Nil, ec = ExecutionContext.global)).thenReturn(Future.successful(section))

      // When
      private val Some(result) = Await.result(curationService.curatedSectionForName("test")(Nil, ec = ExecutionContext.global), 1.second)

      // Then
      result mustBe section
    }

    "return all Stage IDs as Seq(String) when calling curatedStageNamesForName" in new TestScope {
      // Given
      val section = CuratedSection(Seq(CuratedStage("Stage1"), CuratedStage("Stage2")))
      when(mockPapyrusRepo.getByName("test")(Nil, ec = ExecutionContext.global)).thenReturn(Future.successful(section))

      // When
      private val Some(result) = Await.result(curationService.getAllStageNamesBySectionName("test")(Nil, ec = ExecutionContext.global), 1.second)

      // Then
      result mustBe Seq("Stage1", "Stage2")
    }

    "return None when calling curatedStageNamesForName on HttpClientError" in new TestScope {
      // Given
      when(mockPapyrusRepo.getByName("test")(Nil, ec = ExecutionContext.global)).thenThrow(HttpClientErrorException(404, "foo", "foo"))

      // When
      private val result = Await.result(curationService.getAllStageNamesBySectionName("test")(Nil, ec = ExecutionContext.global), 1.second)

      // Then
      result mustBe None
    }

  }

}
