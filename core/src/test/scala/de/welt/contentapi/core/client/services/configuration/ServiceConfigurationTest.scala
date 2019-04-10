package de.welt.contentapi.core.client.services.configuration

import com.typesafe.config.ConfigFactory
import org.scalatestplus.play.PlaySpec

import scala.collection.JavaConverters._

class ServiceConfigurationTest extends PlaySpec {

  "ServiceConfiguration" should {
    "reject service configuration where apiKey and basic auth are both set" in {

      assertThrows[ConfigurationException] {
        ServiceConfiguration.credentialsFromConf(ConfigFactory.parseMap(
          Map(
            ServiceConfiguration.ConfigKeyHost → "http://www.example.com",
            ServiceConfiguration.ConfigKeyEndpoint → "/test/%s",
            ServiceConfiguration.ConfigKeyApiKey → "foo",
            ServiceConfiguration.ConfigKeyBasicUsername → "user",
            ServiceConfiguration.ConfigKeyBasicPassword → "pass"
          ).asJava)
        )
      }
    }
  }
}
