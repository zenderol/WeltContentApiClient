package de.welt

import play.api.inject.Module
import play.api.{Configuration, Environment}

class PlayModule extends Module {

  override def bindings(environment: Environment, configuration: Configuration) = {
    Seq(
      bind[WeltContentClientLogic].to[WeltContentClient].eagerly
    )
  }
}
