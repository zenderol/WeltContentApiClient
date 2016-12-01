package de.welt.contentapi.pressed.client

import de.welt.contentapi.pressed.client.repository.{PressedDiggerClient, PressedDiggerClientImpl, PressedS3Client, PressedS3ClientImpl}
import de.welt.contentapi.pressed.client.services.{PressedContentService, PressedContentServiceImpl, PressedSectionService, PressedSectionServiceImpl}
import play.api.{Configuration, Environment}
import play.api.inject.{Binding, Module}

class PressedModule extends Module {
  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {
    val client = new de.welt.contentapi.core.client.CoreModule()
    client.bindings(environment, configuration) ++ Seq(
      bind(classOf[PressedS3Client]).to(classOf[PressedS3ClientImpl]),
      bind(classOf[PressedDiggerClient]).to(classOf[PressedDiggerClientImpl]),

      bind(classOf[PressedContentService]).to(classOf[PressedContentServiceImpl]),
      bind(classOf[PressedSectionService]).to(classOf[PressedSectionServiceImpl])
    )
  }
}
