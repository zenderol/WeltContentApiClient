package de.welt.contentapi.pressed.client.services

import java.time.Instant
import java.time.temporal.ChronoUnit

import com.google.inject.ImplementedBy
import de.welt.contentapi.core.client.services.CapiExecutionContext
import de.welt.contentapi.core.client.services.configuration.{Environment, Mode}
import de.welt.contentapi.core.client.services.http._
import de.welt.contentapi.pressed.client.repository.{PressedDiggerClient, PressedS3Client}
import de.welt.contentapi.pressed.models.ApiPressedSectionResponse
import de.welt.contentapi.utils.Loggable
import javax.inject.{Inject, Singleton}

import scala.concurrent.Future

@ImplementedBy(classOf[PressedSectionServiceImpl])
trait PressedSectionService {

  def findByPath(path: String)
                (implicit requestHeaders: RequestHeaders): Future[ApiPressedSectionResponse]
}

object PressedSectionService {
  val DefaultSectionTTLMinutes: Long = 30
}

@Singleton
class PressedSectionServiceImpl @Inject()(pressedS3Client: PressedS3Client,
                                          diggerClient: PressedDiggerClient,
                                          implicit val capi: CapiExecutionContext)
  extends PressedSectionService with Loggable {

  val configuredSectionTTL: Long = PressedSectionService.DefaultSectionTTLMinutes

  /**
    * Primarily gets Pressed Section from S3, if pressed is older than 30 minutes or is not present -> fallback to digger rest call
    *
    * @param path path for section to be pressed
    * @return a future ApiPressedSection or deliver HttpClient/ServerError from AbstractService
    */
  override def findByPath(path: String)(implicit requestHeaders: RequestHeaders): Future[ApiPressedSectionResponse] = {
    execute(path, Environment.app, Environment.stage)
  }

  protected[services] def execute(path: String, app: String = Environment.app, stage: Mode = Environment.stage) = {
    if (app == "preview" || stage.isDev) {
      // do not use s3 when rendering a preview or when in dev mode
      diggerClient.findByPath(path)
    } else {

      pressedS3Client.find(path) match {
        case Some((section, lastMod)) if lastMod.plus(configuredSectionTTL, ChronoUnit.MINUTES).isAfter(Instant.now()) ⇒
          // s3 result is present and NOT outdated
          Future.successful(section)
        case Some((section, _)) ⇒
          // s3 result is present, but outdated. Invoke digger, but use outdated result if digger fails
          diggerClient.findByPath(path)
            .recoverWith { case err ⇒
              log.warn("Delivering old content because digger failed.", err)
              Future.successful(section)
            }
        case _ ⇒
          // s3 result is not present, solely rely on digger
          diggerClient.findByPath(path)
      }
    }
  }
}
