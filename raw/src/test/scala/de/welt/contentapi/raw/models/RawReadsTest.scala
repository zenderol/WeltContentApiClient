package de.welt.contentapi.raw.models

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json

class RawReadsTest extends PlaySpec {
  import RawReads._

  private final val emptyJson = "{}"

  "RawChannelCommercialReads" must {

    "create RawChannelCommercial from empty json by using default constructor values" in {
      Json.parse(emptyJson)
        .validate[RawChannelCommercial](rawChannelCommercialReads)
        .asOpt mustBe Some(RawChannelCommercial())
    }

    "add new field `showBiallo` with default value, automatically" in {
      val missingBialloJson = """{"definesAdTag": true, "definesVideoAdTag": true}"""
      Json.parse(missingBialloJson)
      .validate[RawChannelCommercial](rawChannelCommercialReads)
      .asOpt
      .map(_.showBiallo) mustBe Some(RawChannelCommercial().showBiallo)
    }

  }

  "RawChannelTaboolaCommercialReads" must {

    "create RawChannelTaboolaCommercialReads from empty json by using default constructor values" in {
      Json.parse(emptyJson)
        .validate[RawChannelTaboolaCommercial](rawChannelTaboolaCommercialReads)
        .asOpt mustBe Some(RawChannelTaboolaCommercial())
    }

  }
}
