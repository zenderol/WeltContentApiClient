package de.welt.contentapi.pressed.client.services

import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

import de.welt.contentapi.core.client.services.http._
import de.welt.contentapi.pressed.client.repository.{PressedDiggerClient, PressedS3Client}
import de.welt.contentapi.pressed.models.ApiPressedSection
import de.welt.contentapi.utils.Env.{Env, Live, Preview}

import scala.concurrent.{ExecutionContext, Future}

trait PressedSectionService {
  val ttlInMinutes: Long = 30

  def findByPath(path: String, env: Env = Live)
                (implicit requestHeaders: RequestHeaders, executionContext: ExecutionContext): Future[ApiPressedSection]
}

case class PressedSectionServiceImpl @Inject()(pressedS3Client: PressedS3Client,
                                               diggerClient: PressedDiggerClient) extends PressedSectionService {
  /**
    * Primarily gets Pressed Section from S3, if pressed is older than 30 minutes or is not present -> fallback to digger rest call
    *
    * @param path path for section to be pressed
    * @param env  can be Live or Preview, but is Live as default. If (Env == Preview) -> only ask Digger, don't try S3 first
    * @return a future ApiPressedSection or deliver HttpClient/ServerError from AbstractService
    */
  override def findByPath(path: String, env: Env = Live)
                         (implicit requestHeaders: RequestHeaders, executionContext: ExecutionContext): Future[ApiPressedSection] =

  env match {
    case Preview ⇒ diggerClient.findByPath(path)
    case _ ⇒ pressedS3Client.find(path)
      .collect {
        case (section, lastMod) if lastMod.plus(ttlInMinutes, ChronoUnit.MINUTES).isAfter(Instant.now()) ⇒ Future.successful(section)
      }.getOrElse(diggerClient.findByPath(path))
  }

}
