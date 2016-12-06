package de.welt.contentapi.core.client.services.configuration

import play.api.Configuration

/**
  * Reusable Service Configuration object based on the [[play.Configuration]]
  *
  * @param serviceName Name of the Service. Used for building Metrics.
  * @param host        Absolute url the the service host
  * @param endpoint    Relative service endpoint
  * @param username    Basic Auth username
  * @param password    Basic Auth password
  */
case class ServiceConfiguration(serviceName: String, host: String, endpoint: String, username: String, password: String)

object ServiceConfiguration {
  def apply(serviceName: String, configuration: Configuration): ServiceConfiguration =
    configuration.getConfig(serviceName)
      .flatMap { config ⇒
        for {
          host ← config.getString("host")
          endpoint ← config.getString("endpoint")
          username ← config.getString("credentials.username")
          password ← config.getString("credentials.password")
        } yield ServiceConfiguration(serviceName, host, endpoint, username, password)
      } getOrElse {
      throw configuration.reportError(serviceName, s"Service at $serviceName was not configured correctly.")
    }
}
