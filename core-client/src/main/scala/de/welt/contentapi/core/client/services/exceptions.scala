package de.welt.contentapi.core.client.services

import play.api.PlayException
import play.api.http.Status

object exceptions extends Status {

  abstract class HttpStatusCodeException(statusCode: Int, statusPhrase: String, url: String)
    extends PlayException(s"HttpStatusCodeException[$statusCode]", s"$statusPhrase while requesting '$url'") {

    def getStatusCode: Int = statusCode

    def getStatusPhrase: String = statusPhrase

    def getUrl: String = url

    override def toString: String = super.toString
  }

  case class HttpRedirectException(url: String, statusPhrase: String)
    extends HttpStatusCodeException(MOVED_PERMANENTLY, statusPhrase, url)

  case class HttpClientErrorException(statusCode: Int, statusPhrase: String, url: String, cacheHeader: Option[String] = None)
    extends HttpStatusCodeException(statusCode, statusPhrase, url)

  case class HttpServerErrorException(statusCode: Int, statusPhrase: String, url: String)
    extends HttpStatusCodeException(statusCode, statusPhrase, url)

}
