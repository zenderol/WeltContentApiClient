package de.welt.contentapi.core.client.services.configuration

import com.typesafe.config.{Config, ConfigFactory}
import de.welt.contentapi.utils.Loggable
import play.api.http.HttpVerbs

import scala.compat.java8.DurationConverters._
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

sealed trait Credentials

case class BasicAuth(username: BasicUsername, password: BasicPassword) extends Credentials

case class BasicUsername(v: String)

case class BasicPassword(v: String) {
  override def toString: String = "basic(***)"
}

case class ApiKey(v: String) extends Credentials {
  override def toString: String = "api_key(***)"
}

/**
  * Reusable Service Configuration
  *
  * @param serviceName Name of the Service. Used for building Metrics.
  * @param host        Absolute url the the service host
  * @param endpoint    Relative service endpoint
  * @param credentials Either Tuple of Username and Password or an ApiKey
  */
case class ServiceConfiguration(serviceName: String,
                                host: String,
                                endpoint: String,
                                circuitBreaker: CircuitBreakerSettings = CircuitBreakerSettings(),
                                credentials: Option[Credentials] = None,
                                method: String = HttpVerbs.GET)

/**
  * Configure the circuit breaker for a service
  * Example Config:
  *
  * @param delegate Optional config, defaults to ```circuitBreaker disabled```, if none present.
  */
case class CircuitBreakerSettings(enabled: Boolean = CircuitBreakerSettings.Enabled,
                                  maxFailures: Int = CircuitBreakerSettings.MaxFailures,
                                  callTimeout: FiniteDuration = CircuitBreakerSettings.CallTimeout,
                                  resetTimeout: FiniteDuration = CircuitBreakerSettings.ResetTimeout,
                                  exponentialBackoff: FiniteDuration = CircuitBreakerSettings.ExponentialBackoff)

object CircuitBreakerSettings {
  val Enabled: Boolean = false
  val MaxFailures: Int = 5
  val CallTimeout: FiniteDuration = 5.seconds
  val ResetTimeout: FiniteDuration = 10.seconds
  val ExponentialBackoff: FiniteDuration = 2.minutes

  def apply(delegate: Config): CircuitBreakerSettings = CircuitBreakerSettings(
    enabled = Try(delegate.getString("circuit_breaker.enabled") == "true").getOrElse(CircuitBreakerSettings.Enabled),
    maxFailures = Try(delegate.getInt("circuit_breaker.max_failures")).getOrElse(CircuitBreakerSettings.MaxFailures),
    callTimeout = Try(delegate.getDuration("circuit_breaker.call_timeout")).map(_.toScala).getOrElse(CircuitBreakerSettings.CallTimeout),
    resetTimeout = Try(delegate.getDuration("circuit_breaker.reset_timeout")).map(_.toScala).getOrElse(CircuitBreakerSettings.ResetTimeout),
    exponentialBackoff = Try(delegate.getDuration("circuit_breaker.exponential_backoff")).map(_.toScala).getOrElse(CircuitBreakerSettings.ExponentialBackoff)
  )
}

object ServiceConfiguration extends Loggable {

  protected[configuration] val ConfigKeyHost = "host"
  protected[configuration] val ConfigKeyEndpoint = "endpoint"
  protected[configuration] val ConfigKeyMethod = "method"
  protected[configuration] val ConfigKeyBasicUsername = "basic_username"
  protected[configuration] val ConfigKeyBasicPassword = "basic_password"
  protected[configuration] val ConfigKeyApiKey = "api_key"

  protected[configuration] def credentialsFromConf(c: Config): Option[Credentials] = {
    // make sure, that either apiKey or BasicAuth is set. Not both.
    if (c.hasPath(ConfigKeyBasicUsername) && c.hasPath(ConfigKeyApiKey)) {
      ApiConfiguration.reportError(ConfigKeyBasicUsername, "Both `api_key` and basic auth `credentials` were configured. Please remove one of those.")
    }

    (for {
      username ← Try(c.getString(ConfigKeyBasicUsername)).map(BasicUsername)
      password ← Try(c.getString(ConfigKeyBasicPassword)).map(BasicPassword)
    } yield BasicAuth(username, password))
      .orElse(Try(c.getString(ConfigKeyApiKey)).map(v ⇒ ApiKey(v)))
      .toOption
  }

  def apply(serviceName: String): ServiceConfiguration = {
    val triedConfig = Try(ApiConfiguration.configuration.getConfig("api." + serviceName))
      .flatMap { config ⇒
        for {
          host ← Try(config.getString(ConfigKeyHost))
          endpoint ← Try(config.getString(ConfigKeyEndpoint))
        } yield ServiceConfiguration(serviceName, host, endpoint,
          credentials = credentialsFromConf(config),
          circuitBreaker = CircuitBreakerSettings(config),
          method = Try(config.getString(ConfigKeyMethod)).getOrElse(HttpVerbs.GET)
        )
      }
    triedConfig match {
      case Success(value) ⇒
        log.debug(s"Configured Service('$serviceName') -> $value")
        value
      case Failure(th) ⇒
        log.error("Could not configure Service.", th)
        ApiConfiguration.reportError("api." + serviceName, s"Service '$serviceName' was not configured correctly.", th)
    }
  }
}