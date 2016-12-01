package de.welt.contentapi.core.client

import de.welt.contentapi.core.client.services.contentapi._
import de.welt.contentapi.core.client.services.s3.{S3Client, S3ClientImpl}
import play.api.inject.{Binding, Module}
import play.api.{Configuration, Environment}

class CoreModule extends Module {

  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {
    Seq(
      bind(classOf[S3Client]).to(classOf[S3ClientImpl]),

      bind(classOf[ContentService]).to(classOf[ContentServiceImpl]),
      bind(classOf[ContentSearchService]).to(classOf[ContentSearchServiceImpl])
    )
  }
}
