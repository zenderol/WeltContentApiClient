package de.welt.contentapi.pressed.client.services

import de.welt.contentapi.pressed.client.converter.RawToApiConverter
import de.welt.contentapi.pressed.models.{ApiChannel, ApiConfiguration}
import de.welt.contentapi.raw.client.services.RawTreeService
import de.welt.contentapi.raw.models.{RawChannel, RawChannelId}
import org.mockito.Mockito
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec

class TreeServiceTest extends PlaySpec with MockitoSugar {

  private trait Scope {
    val rts = mock[RawTreeService]
    val converter = mock[RawToApiConverter]
    val treeService: TreeService = new TreeServiceImpl(rts, converter)
  }

  "TreeService" should {


    "pass data for existing sections" in new Scope {
      val channel = RawChannel(RawChannelId("/", "root", 0))

      Mockito.when(rts.root).thenReturn(Some(channel))
      val apiConfig = ApiConfiguration()
      Mockito.when(converter.apiConfigurationFromRawChannel(channel)).thenReturn(apiConfig)
      val apiChannel = ApiChannel()
      Mockito.when(converter.apiChannelFromRawChannel(channel)).thenReturn(apiChannel)

      val response = treeService.find("/")

      response mustBe Some(apiChannel → apiConfig)
      Mockito.verify(converter).apiConfigurationFromRawChannel(channel)
      Mockito.verify(converter).apiChannelFromRawChannel(channel)
    }
  }
}
