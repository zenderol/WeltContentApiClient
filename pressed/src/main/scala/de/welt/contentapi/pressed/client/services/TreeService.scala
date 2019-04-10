package de.welt.contentapi.pressed.client.services

import com.google.inject.ImplementedBy
import de.welt.contentapi.pressed.client.converter.RawToApiConverter
import de.welt.contentapi.pressed.models.{ApiChannel, ApiConfiguration}
import de.welt.contentapi.raw.client.services.RawTreeService
import javax.inject.{Inject, Singleton}

/**
  * access non-pressed section information (without invoking digger) to obtain
  * section information and its configuration
  */
@ImplementedBy(classOf[TreeServiceImpl])
trait TreeService {
  def find(path: String): Option[(ApiChannel, ApiConfiguration)]
}

@Singleton
class TreeServiceImpl @Inject()(rawTreeServiceImpl: RawTreeService,
                                converter: RawToApiConverter) extends TreeService {

  def find(path: String): Option[(ApiChannel, ApiConfiguration)] = {

    rawTreeServiceImpl.root
      .flatMap(_.findByPath(path))
      .map { ch ⇒

        val apiChannel = converter.apiChannelFromRawChannel(ch)
        val apiConfiguration = converter.apiConfigurationFromRawChannel(ch)

        apiChannel → apiConfiguration
      }
  }
}
