package de.welt.contentapi.core.client

import akka.actor.ActorSystem
import com.google.inject.{AbstractModule, Provides, Singleton}
import com.kenshoo.play.metrics.{Metrics, MetricsImpl}
import de.welt.contentapi.core.client.services.CapiExecutionContext
import de.welt.contentapi.core.client.services.contentapi._
import de.welt.contentapi.core.client.services.s3.{S3Client, S3ClientImpl}

class CoreModule extends AbstractModule {

  override def configure() = {
    bind(classOf[Metrics]).to(classOf[MetricsImpl])
  }

  @Provides @Singleton
  def capiContext(actorSystem: ActorSystem): CapiExecutionContext = CapiExecutionContext(actorSystem, "contexts.capi")
}
