package de.welt.contentapi.raw.models

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json

class RawReadsTest extends PlaySpec {
  import RawReads._

  "rawChannelCommercialReads" must {

    "add new field `showBiallo` with default value, automatically" in {

      val missingBialloSettingJson = """{"definesAdTag": true, "definesVideoAdTag": true}"""

      Json.parse(missingBialloSettingJson)
        .validate[RawChannelCommercial](rawChannelCommercialReads)
        .asOpt
        .map(_.showBiallo) mustBe Some(RawChannelCommercial().showBiallo)
    }

    "empty RawChannelCommercial json config leads to default constructor values" in {
      val emptyJson = "{}"

      Json.parse(emptyJson)
        .validate[RawChannelCommercial](rawChannelCommercialReads)
        .asOpt mustBe Some(RawChannelCommercial())
    }
  }
}
