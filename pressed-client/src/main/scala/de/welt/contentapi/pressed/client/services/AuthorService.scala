package de.welt.contentapi.pressed.client.services

import java.time.Instant

import com.google.inject.{ImplementedBy, Inject, Singleton}
import de.welt.contentapi.core.client.services.CapiExecutionContext
import de.welt.contentapi.core.client.services.s3.S3Client
import de.welt.contentapi.core.models.ApiContent
import de.welt.contentapi.core.models.ApiReads.apiContentReads
import de.welt.contentapi.pressed.models.ApiPressedContent
import de.welt.contentapi.utils.Loggable
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.{Configuration, Environment, Logger, Mode}

import scala.concurrent.duration._

/**
  * provides access to all authors
  *
  * authors are persisted in s3 (via the author backend `abe`)
  */
@ImplementedBy(classOf[AuthorServiceImpl])
trait AuthorService {
  /**
    * All existing authors currently known.
    *
    * Those are read from S3, the data originates from `abe` ("_A_uthor _B_ack _E_nd" λ) who executes
    * an search against the search back end `doug`.
    *
    * Will be periodically updated via cron.
    *
    * @return a seq of all know authors
    */
  def allAuthors(): Seq[ApiPressedContent]

  /**
    * Search for an author by its `webURL`.
    *
    * @param webURL the URL of the author used in the web, e.g. `/autor/frederick-schwilden/`
    * @return an author if found, `None` otherwise
    */
  def findByWebURL(webURL: String): Option[ApiPressedContent]

  /**
    * search for an author by its escenic id
    *
    * @param id the numeric ece id
    * @return an author if found, `None` otherwise
    */
  def findByEceId(id: String): Option[ApiPressedContent]
}

@Singleton
class AuthorServiceImpl @Inject()(s3Client: S3Client,
                                  config: Configuration,
                                  pcs: PressedContentService,
                                  environment: Environment,
                                  implicit val capi: CapiExecutionContext)
  extends AuthorService with Loggable {

  private val Bucket = config.get[String](AuthorServiceConstants.S3BucketConfigKey)
  private val File = config.get[String](AuthorServiceConstants.S3FileConfigKey)

  log.info(s"AuthorService s3://$Bucket/$File")

  protected[services] var _allAuthors: Seq[ApiPressedContent] = Seq.empty
  protected[services] var lastModified: Instant = Instant.MIN

  // instantly start fetching authors
  if (Mode.Test != environment.mode) {
    fetchAuthorsFromS3()
    // schedule future updates of the authors
    capi.actorSystem.scheduler.schedule(10.minutes, 10.minutes, new Runnable {
      override def run(): Unit = fetchAuthorsFromS3()
    })
  } else {
    Logger.info("Authors will not be loaded when started in Mode.Test. If you require author data, please mock it.")
  }

  private def fetchAuthorsFromS3() = {
    val maybeLastMod = s3Client.getLastModified(Bucket, File)
    log.info(s"Starting AuthorService.fetchAuthorsFromS3(), lastmod= $maybeLastMod")
    for {
      remoteLastModified ← maybeLastMod
      if lastModified != remoteLastModified
      authorsAsText ← s3Client.get(Bucket, File)
    } yield {
      val maybeAuthors: Option[List[ApiContent]] = Json.parse(authorsAsText).validate[List[ApiContent]] match {
        case JsSuccess(v, _) ⇒ Some(v)
        case err@JsError(_) ⇒
          log.error("Could not parse authors from JSON" + err.toString)
          None
      }

      maybeAuthors.foreach { authors ⇒

        _allAuthors = authors.map(author ⇒ pcs.convert(author))
        lastModified = remoteLastModified

        log.info(s"Finished AuthorService.fetchAuthorsFromS3(). Found ${authors.size} authors. LastMod $lastModified -> $remoteLastModified")
      }
    }
  }

  override def allAuthors(): Seq[ApiPressedContent] = _allAuthors

  override def findByWebURL(webURL: String): Option[ApiPressedContent] = _allAuthors.find(_.content.webUrl == webURL)

  override def findByEceId(id: String): Option[ApiPressedContent] = _allAuthors.find(_.content.id.contains(id))
}

object AuthorServiceConstants {
  val S3BucketConfigKey: String = "welt.aws.s3.authors.bucket"
  val S3FileConfigKey: String = "welt.aws.s3.authors.file"
}