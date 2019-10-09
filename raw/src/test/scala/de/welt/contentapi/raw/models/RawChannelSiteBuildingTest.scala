package de.welt.contentapi.raw.models

import org.scalatestplus.play.PlaySpec

class RawChannelSiteBuildingTest extends PlaySpec {

  "unwrappedFields()" should {

    "filter empty values" in {
      RawChannelSiteBuilding(fields = Some(Map("sponsoring_slogan" -> ""))).headerFields mustBe Map.empty
    }
  }

  "isEmpty()" should {

    "be true if has constructor values" in {
      RawChannelSiteBuilding().isEmpty mustBe true
    }

    "be true if only `fields` are present but they are empty" in {
      RawChannelSiteBuilding(fields = Some(Map.empty)).isEmpty mustBe true
    }

  }

  "headerFields()" should {

    "return only fields that start with `header_`" in {
      RawChannelSiteBuilding(fields = Some(Map(
        "sponsoring_slogan" -> "Winamp - it really whips the llama's ass",
        "header_logo" -> "zukunftsfond",
        "header_hidden" -> "true"
      ))).headerFields mustBe Map(
        "header_logo" -> "zukunftsfond",
        "header_hidden" -> "true"
      )
    }

    "return only `header_` fields with values other than `empty String`" in {
      RawChannelSiteBuilding(fields = Some(Map(
        "header_logo" -> "",
        "header_hidden" -> "true"
      ))).headerFields mustBe Map(
        "header_hidden" -> "true"
      )
    }
  }

  "sponsoringFields()" should {

    "return only fields that start with `sponsoring_`" in {
      RawChannelSiteBuilding(fields = Some(Map(
        "sponsoring_slogan" -> "Winamp - it really whips the llama's ass",
        "sponsoring_hidden" -> "true",
        "header_logo" -> "zukunftsfond",
        "header_hidden" -> "true"
      ))).sponsoringFields mustBe Map(
        "sponsoring_slogan" -> "Winamp - it really whips the llama's ass",
        "sponsoring_hidden" -> "true"
      )
    }

    "return only `sponsoring_` fields with values other than `empty String`" in {
      RawChannelSiteBuilding(fields = Some(Map(
        "sponsoring_slogan" -> "",
        "sponsoring_hidden" -> "true"
      ))).sponsoringFields mustBe Map(
        "sponsoring_hidden" -> "true"
      )
    }
  }

  // CMCF keeps its internal state in a Map[String,String]
  // And because within the internal state the header can only be hidden or not, its never undefined, and therefore at least this header field is present
  // so we have to do a validation which header fields are contained, and if its only `header_hidden = false` we say that the header is actually empty
  "emptyHeader()" should {

    "return true if `headerFields()` only returns `header_hidden = false` " in {
      RawChannelSiteBuilding(fields = Some(Map(
        "sponsoring_slogan" -> "Winamp - it really whips the llama's ass",
        "sponsoring_hidden" -> "true",
        "header_hidden" -> "false"
      ))).emptyHeader mustBe true
    }

    "return true if `headerFields()` returns an empty Map " in {
      RawChannelSiteBuilding(fields = Some(Map(
        "sponsoring_slogan" -> "Winamp - it really whips the llama's ass",
        "sponsoring_hidden" -> "true"
      ))).emptyHeader mustBe true
    }
  }

  "emptySponsoring()" should {
    "return true if no field starts with `sponsoring_`" in {
      RawChannelSiteBuilding(fields = Some(Map(
        "header_logo" -> "zukunftsfond",
        "header_hidden" -> "true"
      ))).emptySponsoring mustBe true

    }
  }


}
