package de.welt.contentapi.core.client.services

object http {

  /**
    * We need some request headers for logging (X-Unique-ID)
    * Better readability of service parameter.
    */
  type RequestHeaders = Seq[(String, String)]
}
