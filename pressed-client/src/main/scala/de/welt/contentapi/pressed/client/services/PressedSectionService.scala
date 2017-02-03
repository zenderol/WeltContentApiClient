package de.welt.contentapi.pressed.client.services

import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.{Inject, Singleton}

import de.welt.contentapi.core.client.services.http._
import de.welt.contentapi.pressed.client.repository.{PressedDiggerClient, PressedS3Client}
import de.welt.contentapi.pressed.models.ApiPressedSectionResponse
import de.welt.contentapi.utils.Env.{Env, Live, Preview}
import de.welt.contentapi.utils.Loggable
import play.api.Mode.Mode
import play.api.{Configuration, Mode}

import scala.concurrent.{ExecutionContext, Future}

trait PressedSectionService {

  def findByPath(path: String, env: Env = Live, mode: Mode = Mode.Prod)
                (implicit requestHeaders: RequestHeaders, executionContext: ExecutionContext): Future[ApiPressedSectionResponse]
}

object PressedSectionService {
  val DefaultSectionTTLMinutes: Long = 30
}

@Singleton
class PressedSectionServiceImpl @Inject()(pressedS3Client: PressedS3Client,
                                          configuration: Configuration,
                                          diggerClient: PressedDiggerClient) extends PressedSectionService with Loggable {

  lazy val configuredSectionTTL = configuration.getLong("welt.api.digger.ttlMinutes").getOrElse(PressedSectionService.DefaultSectionTTLMinutes)

  /**
    * Primarily gets Pressed Section from S3, if pressed is older than 30 minutes or is not present -> fallback to digger rest call
    *
    * @param path path for section to be pressed
    * @param env  can be Live or Preview, but is Live as default. If (Env == Preview) -> only ask Digger, don't try S3 first
    * @return a future ApiPressedSection or deliver HttpClient/ServerError from AbstractService
    */
  override def findByPath(path: String, env: Env = Live, mode: Mode = Mode.Prod)
                         (implicit requestHeaders: RequestHeaders, executionContext: ExecutionContext): Future[ApiPressedSectionResponse] = {

    if (env == Preview) {
      // do not use s3 when rendering a preview
      diggerClient.findByPath(path, Preview)
    } else if (mode == Mode.Dev) {
      // also do not use s3 when in dev mode
      diggerClient.findByPath(path, Live)
    } else {

      pressedS3Client.find(path) match {
        case Some((section, lastMod)) if lastMod.plus(configuredSectionTTL, ChronoUnit.MINUTES).isAfter(Instant.now()) ⇒
          // s3 result is present and NOT outdated
          Future.successful(section)
        case Some((section, _)) ⇒
          // s3 result is present, but outdated. Invoke digger, but use outdated result if digger fails
          diggerClient.findByPath(path, Live)
            .recoverWith { case err ⇒
              log.warn("Delivering old content because digger failed.", err)
              Future.successful(section)
            }
        case _ ⇒
          // s3 result is not present, solely rely on digger
          diggerClient.findByPath(path, Live)
      }
    }
  }
}
