package de.welt.contentapi.legacy

import com.google.inject.AbstractModule
import de.welt.contentapi.legacy.client.{LegacySectionService, LegacySectionServiceImpl}

class LegacyModule extends AbstractModule {

  override def configure() = {

    bind(classOf[LegacySectionService]).to(classOf[LegacySectionServiceImpl])
  }
}
