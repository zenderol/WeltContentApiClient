package de.welt.contentapi.core.client.services

import akka.actor.ActorSystem
import play.api.libs.concurrent.CustomExecutionContext

case class CapiExecutionContext(actorSystem: ActorSystem, contextName: String)
  extends CustomExecutionContext(actorSystem, contextName)
