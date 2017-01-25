package de.welt.contentapi.raw.client

import com.google.inject.AbstractModule
import de.welt.contentapi.raw.client.services._

class RawClientModule extends AbstractModule {

  override def configure() = {

    install(new de.welt.contentapi.core.client.CoreModule())
    // admin services
    bind(classOf[RawTreeService]).to(classOf[RawTreeServiceImpl])
    bind(classOf[AdminSectionService]).to(classOf[AdminSectionServiceImpl])
    bind(classOf[SdpSectionDataService]).to(classOf[SdpSectionDataServiceImpl])
  }
}
