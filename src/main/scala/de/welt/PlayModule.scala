package de.welt

import de.welt.services.configuration.{ContentClientConfig, ContentClientConfigImpl, SectionConfigurationService, SectionConfigurationServiceImpl}
import de.welt.services.contentapi._
import de.welt.services.s3.{S3, S3Impl}
import play.api.inject.Module
import play.api.{Configuration, Environment}

class PlayModule extends Module {

  override def bindings(environment: Environment, configuration: Configuration) = {
    Seq(
        bind(classOf[ContentClientConfig]).to(classOf[ContentClientConfigImpl]),
        bind(classOf[S3]).to(classOf[S3Impl]),

        bind(classOf[ContentService]).to(classOf[ContentServiceImpl]),
        bind(classOf[ContentEnrichmentService]).to(classOf[ContentEnrichmentServiceImpl]),
        bind(classOf[SectionConfigurationService]).to(classOf[SectionConfigurationServiceImpl]),
        bind(classOf[ContentSearchService]).to(classOf[ContentSearchServiceImpl]),

        bind[WeltContentClientLogic].to[WeltContentClient].eagerly
    )
  }
}
