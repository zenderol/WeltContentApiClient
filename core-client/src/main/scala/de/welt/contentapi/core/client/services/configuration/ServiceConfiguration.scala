package de.welt.contentapi.core.client.services.configuration

import de.welt.contentapi.core.client.services.configuration.Authentication.{ApiKey, Password, Username}
import play.api.Configuration

import scala.concurrent.duration._

object Authentication {
  type ApiKey = String
  type Username = String
  type Password = String
}

/**
  * Reusable Service Configuration object based on the [[play.api.Configuration]]
  *
  * Example config:
  *
  * {{{
  * host: https://www.example.com
  * endpoint: /item/%s
  * apiKey: dead-beef-1234567-1234-12345567
  * circuitBreaker {
  *   enabled: false
  *   maxFailures: 5
  *   callTimeout: 5 seconds
  *   resetTimeout: 10 seconds
  *   exponentialBackoff: 2 minutes
  * }
  * }}}
  *
  * @param serviceName Name of the Service. Used for building Metrics.
  * @param host        Absolute url the the service host
  * @param endpoint    Relative service endpoint
  * @param credentials Either Tuple of Username and Password or an ApiKey
  */
case class ServiceConfiguration(serviceName: String,
                                host: String,
                                endpoint: String,
                                credentials: Either[ApiKey, (Username, Password)],
                                circuitBreaker: CircuitBreakerSettings)

/**
  * Configure the circuit breaker for a service
  * Example Config:
  *
  * @param delegate Optional config, defaults to ```circuitBreaker disabled```, if none present.
  */
case class CircuitBreakerSettings(delegate: Configuration) {
  lazy val enabled: Boolean = delegate.getOptional[Boolean]("circuitBreaker.enabled")
    .getOrElse(CircuitBreakerDefaults.Enabled)
  lazy val maxFailures: Int = delegate.getOptional[Int]("circuitBreaker.maxFailures")
    .getOrElse(CircuitBreakerDefaults.MaxFailures)
  lazy val callTimeout: FiniteDuration = delegate.getOptional[FiniteDuration]("circuitBreaker.callTimeout")
    .getOrElse(CircuitBreakerDefaults.CallTimeout)
  lazy val resetTimeout: FiniteDuration = delegate.getOptional[FiniteDuration]("circuitBreaker.resetTimeout")
    .getOrElse(CircuitBreakerDefaults.ResetTimeout)
  lazy val exponentialBackoff: FiniteDuration = delegate.getOptional[FiniteDuration]("circuitBreaker.exponentialBackoff")
    .getOrElse(CircuitBreakerDefaults.ExponentialBackoff)
}

object CircuitBreakerDefaults {
  val Enabled: Boolean = false
  val MaxFailures: Int = 5
  val CallTimeout: FiniteDuration = 5.seconds
  val ResetTimeout: FiniteDuration = 10.seconds
  val ExponentialBackoff: FiniteDuration = 2.minutes
}

object ServiceConfiguration {

  private val Host = "host"
  private val Endpoint = "endpoint"
  private val Credentials = "credentials"
  private val Username = "username"
  private val Password = "password"
  private val ApiKey = "apiKey"

  private def credentialsFromConf(c: Configuration): Option[Either[ApiKey, (Username, Password)]] = {
    // make sure, that either apiKey or BasicAuth is set. Not both.
    if (c.has(Credentials) && c.has(ApiKey)) {
      throw c.reportError("credentials", "Both `apiKey` and basic auth `credentials` were configured. Please remove one of those.")
    }

    (
      for {
        credentials ← c.getOptional[Configuration](Credentials)
        username ← credentials.getOptional[String](Username)
        password ← credentials.getOptional[String](Password)
        password ← credentials.getOptional[String](Password)
      } yield Right((username, password))
    ).orElse(c.getOptional[String](ApiKey).map(Left(_)))
  }

  private def printServiceConfig(c: Configuration): Set[(String, String)] = c.entrySet.flatMap {
    case (ApiKey, _) ⇒ Set(ApiKey → "***")
    case (Password, _) ⇒  Set(Password → "***")
    case (Credentials, value: Configuration) ⇒ printServiceConfig(value).map { case (k, v) ⇒ s"credentials.$k" → v }
    case (key, value) ⇒  Set(key → value.toString)
  }

  def apply(serviceName: String, configuration: Configuration): ServiceConfiguration =
    configuration.getOptional[Configuration](serviceName).flatMap { config ⇒
      for {
        host ← config.getOptional[String](Host)
        endpoint ← config.getOptional[String](Endpoint)
        credentials ← credentialsFromConf(config)
      } yield ServiceConfiguration(serviceName, host, endpoint, credentials, CircuitBreakerSettings(config))
    } getOrElse {
      val debug = printServiceConfig(configuration.getOptional[Configuration](serviceName).getOrElse(Configuration()))
      throw configuration.reportError(serviceName, s"Service '$serviceName' was not configured correctly. $debug")
    }
}