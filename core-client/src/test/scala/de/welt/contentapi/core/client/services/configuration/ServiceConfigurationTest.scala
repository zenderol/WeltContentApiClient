package de.welt.contentapi.core.client.services.configuration

import org.scalatestplus.play.PlaySpec
import play.api.{Configuration, PlayException}

class ServiceConfigurationTest extends PlaySpec {

  "ServiceConfiguration" should {
    "reject service configuration where apiKey and basic auth are both set" in {

      assertThrows[PlayException] {
        ServiceConfiguration("test", Configuration("test" → Map(
          "host" → "http://www.example.com",
          "endpoint" → "/test/%s",
          "apiKey" → "foo",
          "credentials.username" → "user",
          "credentials.password" → "pass"
        )))
      }
    }
  }
}
