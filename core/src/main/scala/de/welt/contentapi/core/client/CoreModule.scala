package de.welt.contentapi.core.client

import akka.actor.ActorSystem
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.{AmazonS3, AmazonS3Client}
import com.google.inject.{AbstractModule, Provides, Singleton}
import com.kenshoo.play.metrics.{Metrics, MetricsImpl}
import de.welt.contentapi.core.client.services.CapiExecutionContext
import de.welt.contentapi.core.client.services.configuration.{ApiConfiguration, Environment, Mode}
import de.welt.contentapi.utils.Loggable

import scala.util.{Failure, Success, Try}

class CoreModule extends AbstractModule with Loggable {

  override def configure() = {
    bind(classOf[Metrics]).to(classOf[MetricsImpl])
  }

  @Provides
  @Singleton
  def capiContext(actorSystem: ActorSystem): CapiExecutionContext = CapiExecutionContext(actorSystem, "contexts.capi")

  @Provides
  @Singleton
  def s3client(mode: Mode = Environment.stage): AmazonS3 =
    if (mode.isTest) {
      log.error("[CommonModule] Please mock the S3Client when running in Test mode.")
      throw new RuntimeException("Please mock the S3Client when running in Test mode.")
    } else {
      ApiConfiguration.aws.credentials.map { credentials ⇒
        val region: Regions = Try(ApiConfiguration.aws.s3.region).map(Regions.fromName).getOrElse(Regions.EU_WEST_1)
        log.debug(s"s3 connected to $region")
        AmazonS3Client.builder()
          .withCredentials(credentials)
          .withRegion(region)
          .build()
      } match {
        case Success(value) ⇒ value
        case Failure(th) ⇒
          log.error("Could not initialize AWS S3 Client.", th)
          throw th
      }
    }

  @Provides
  def mode(): Mode = Environment.stage
}
