package de.welt.contentapi.raw.client

import de.welt.contentapi.raw.client.services._
import play.api.inject.Module
import play.api.{Configuration, Environment}

class RawClientModule extends Module {

  override def bindings(environment: Environment, configuration: Configuration) = {
    val client = new de.welt.contentapi.core.client.CoreModule()
    client.bindings(environment, configuration) ++ Seq(
      // admin services
      bind(classOf[RawTreeService]).to(classOf[RawTreeServiceImpl]),
      bind(classOf[AdminSectionService]).to(classOf[AdminSectionServiceImpl]),
      bind(classOf[SdpSectionDataService]).to(classOf[SdpSectionDataServiceImpl])
    )
  }
}
