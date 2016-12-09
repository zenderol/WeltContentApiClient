package de.welt.contentapi.raw.models

import org.scalatestplus.play.PlaySpec

class RawInheritenceTest extends PlaySpec {

  "ContentConfiguration" should {

    "be None for root section without Config" in new TestScope {
      rootNoConfig.getMaybeContentOverrides mustBe None
    }

    "inherit from parent /sport/ to channel /sport/fussball/" in new TestScope {
      fussball.getMaybeContentOverrides mustBe sport.config.content
    }

    "be Some for /sport/" in new TestScope {
      sport.getMaybeContentOverrides mustBe sport.config.content
    }

    "inherit over more than one level for /sport/fussball/bundesliga/" in new TestScope {
      bundesliga.getMaybeContentOverrides mustBe sport.config.content
    }
  }

}


trait TestScope {

  val rootNoConfig: RawChannel = RawChannel(
    id = RawChannelId(
      path = "/",
      label = "home",
      escenicId = 6
    ),
    config = RawChannelConfiguration(
      content = None
    )
  )

  val bundesliga: RawChannel = RawChannel(
    id = RawChannelId(
      path = "/sport/fussball/bundesliga/",
      label = "bundesliga",
      escenicId = 5000
    ),
    config = RawChannelConfiguration(
      content = None
    )
  )
  val fussball: RawChannel = RawChannel(
    id = RawChannelId(
      path = "/sport/fussball/",
      label = "fussball",
      escenicId = 500
    ),
    config = RawChannelConfiguration(
      content = None
    ),
    children = Seq(bundesliga)
  )
  val sport: RawChannel = RawChannel(
    id = RawChannelId(
      path = "/sport/",
      label = "sport",
      escenicId = 50
    ),
    config = RawChannelConfiguration(
      content = Some(RawChannelContentConfiguration(
        subTypeQueryForText = Some("-ticker"),
        typeQueryForText = Some("article"),
        subTypeQueryForVideo = Some("-broadcast"),
        typeQueryForVideo = Some("video")
      ))
    ), children = Seq(fussball)
  )

  val root: RawChannel = RawChannel(
    id = RawChannelId(
      path = "/",
      label = "home",
      escenicId = 5
    ),
    config = RawChannelConfiguration(
      content = Some(RawChannelContentConfiguration(
        subTypeQueryForText = Some("-ticker"),
        typeQueryForText = Some("article"),
        subTypeQueryForVideo = Some("-broadcast"),
        typeQueryForVideo = Some("video")
      ))
    ),
    children = Seq(sport)
  )

  root.updateParentRelations()

}