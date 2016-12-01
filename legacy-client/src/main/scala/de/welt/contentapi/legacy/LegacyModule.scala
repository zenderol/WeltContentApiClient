package de.welt.contentapi.legacy

import de.welt.contentapi.legacy.client.{LegacySectionService, LegacySectionServiceImpl}
import play.api.inject.{Binding, Module}
import play.api.{Configuration, Environment}

class LegacyModule extends Module {
  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] =
    Seq(
      bind(classOf[LegacySectionService]).to(classOf[LegacySectionServiceImpl])
    )
}
