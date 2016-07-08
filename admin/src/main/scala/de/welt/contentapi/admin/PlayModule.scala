package de.welt.contentapi.admin

import de.welt.contentapi.admin.services.{AdminSectionService, AdminSectionServiceImpl}
import de.welt.contentapi.client.services.contentapi.admin.{LegacySectionService, LegacySectionServiceImpl}
import play.api.inject.Module
import play.api.{Configuration, Environment}

class PlayModule extends Module {

  override def bindings(environment: Environment, configuration: Configuration) = {

    val client = new de.welt.contentapi.client.PlayModule()

    client.bindings(environment, configuration) ++ Seq(
      // admin services
      bind(classOf[AdminSectionService]).to(classOf[AdminSectionServiceImpl]),
      bind(classOf[LegacySectionService]).to(classOf[LegacySectionServiceImpl])

    )
  }
}
