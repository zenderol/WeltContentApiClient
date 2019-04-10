package de.welt.contentapi

import akka.actor.ActorSystem
import de.welt.contentapi.core.client.services.CapiExecutionContext

trait TestExecutionContext {
  private val actorSystem = ActorSystem("test")
  implicit val executionContext = new CapiExecutionContext(actorSystem, "contexts.test")
}

object TestExecutionContext extends TestExecutionContext