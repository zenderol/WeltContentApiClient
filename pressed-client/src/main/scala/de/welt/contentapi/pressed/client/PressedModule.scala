package de.welt.contentapi.pressed.client

import com.google.inject.AbstractModule
import de.welt.contentapi.pressed.client.repository.{PressedDiggerClient, PressedDiggerClientImpl, PressedS3Client, PressedS3ClientImpl}
import de.welt.contentapi.pressed.client.services.{PressedContentService, PressedContentServiceImpl, PressedSectionService, PressedSectionServiceImpl}

class PressedModule extends AbstractModule {

  override def configure() = {

    install(new de.welt.contentapi.core.client.CoreModule())

    bind(classOf[PressedS3Client]).to(classOf[PressedS3ClientImpl])
    bind(classOf[PressedDiggerClient]).to(classOf[PressedDiggerClientImpl])
    bind(classOf[PressedContentService]).to(classOf[PressedContentServiceImpl])
    bind(classOf[PressedSectionService]).to(classOf[PressedSectionServiceImpl])

  }
}
