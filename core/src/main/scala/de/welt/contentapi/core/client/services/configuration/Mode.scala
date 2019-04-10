package de.welt.contentapi.core.client.services.configuration

sealed trait Mode {
  val isProd: Boolean = false
  val isStaging: Boolean = false
  val isDev: Boolean = false
  val isTest: Boolean = false
}

case object Development extends Mode {
  override val isDev: Boolean = true
}

case object Test extends Mode {
  override val isTest: Boolean = true
}

case object Staging extends Mode {
  override val isStaging: Boolean = true
}

case object Production extends Mode {
  override val isProd: Boolean = true
}
