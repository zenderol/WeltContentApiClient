package de.welt.contentapi.core.client

import com.google.inject.AbstractModule
import com.kenshoo.play.metrics.{Metrics, MetricsImpl}
import de.welt.contentapi.core.client.services.contentapi._
import de.welt.contentapi.core.client.services.s3.{S3Client, S3ClientImpl}

class CoreModule extends AbstractModule {

  override def configure() = {

    bind(classOf[Metrics]).to(classOf[MetricsImpl])
    bind(classOf[S3Client]).to(classOf[S3ClientImpl])
    bind(classOf[ContentService]).to(classOf[ContentServiceImpl])
    bind(classOf[ContentSearchService]).to(classOf[ContentSearchServiceImpl])
  }
}
