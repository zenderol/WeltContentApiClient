package de.welt.contentapi.client.services

object exceptions {

  class BadConfigurationException(msg: String) extends RuntimeException(msg)

  class NotFoundException(message: String) extends RuntimeException(message: String) {
    override def getMessage: String = s"Not Found (HTTP 404) $message"
  }

  class ServerError(message: String) extends RuntimeException(message: String)

}
