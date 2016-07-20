package de.welt.contentapi.client.services

import play.api.PlayException

object exceptions {

  class BadConfigurationException(msg: String) extends RuntimeException(msg)

  abstract class HttpStatusCodeException(statusCode: Int, statusPhrase: String, url: String)
    extends PlayException(s"HttpStatusCodeException[$statusCode]", statusPhrase) {
    def getStatusCode = statusCode
    def getStatusPhrase = statusPhrase
    def getUrl = url

    override def toString: String = super.toString
  }

  case class HttpClientErrorException(statusCode: Int, statusPhrase: String, url: String) extends HttpStatusCodeException(statusCode, statusPhrase, url)
  case class HttpServerErrorException(statusCode: Int, statusPhrase: String, url: String) extends HttpStatusCodeException(statusCode, statusPhrase, url)

}
