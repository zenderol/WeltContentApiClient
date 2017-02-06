package de.welt.contentapi.core.client.services.configuration

import de.welt.contentapi.core.client.services.configuration.Authentication.{ApiKey, Password, Username}
import play.api.Configuration

object Authentication {
  type ApiKey = String
  type Username = String
  type Password = String
}

/**
  * Reusable Service Configuration object based on the [[play.Configuration]]
  *
  * @param serviceName Name of the Service. Used for building Metrics.
  * @param host        Absolute url the the service host
  * @param endpoint    Relative service endpoint
  * @param credentials Either Tuple of Username and Password or an ApiKey
  */
case class ServiceConfiguration(serviceName: String, host: String, endpoint: String, credentials: Either[ApiKey, (Username, Password)])

object ServiceConfiguration {
  def apply(serviceName: String, configuration: Configuration): ServiceConfiguration =
    configuration.getConfig(serviceName)
      .flatMap { config ⇒

        config match {
          case c if c.getString("apiKey").isDefined ⇒ for {
            host ← config.getString("host")
            endpoint ← config.getString("endpoint")
            apiKey ← config.getString("apiKey")
          } yield ServiceConfiguration(serviceName, host, endpoint, Left(apiKey))
          case _ ⇒ for {
            host ← config.getString("host")
            endpoint ← config.getString("endpoint")
            username ← config.getString("credentials.username")
            password ← config.getString("credentials.password")
          } yield {
            ServiceConfiguration(serviceName, host, endpoint, Right((username, password)) )
          }
        }

      } getOrElse {
      throw configuration.reportError(serviceName, s"Service '$serviceName' was not configured correctly.")
    }
}
