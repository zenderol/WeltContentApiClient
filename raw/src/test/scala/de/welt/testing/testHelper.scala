package de.welt.testing

import de.welt.contentapi.raw.models.{RawChannel, RawChannelCommercial, RawChannelConfiguration, RawChannelId, RawChannelMetadata}

object testHelper {

  object raw {

    object channel {
      def emptyWithId(id: Long) =
        RawChannel(RawChannelId(path = "", escenicId = id, label = id.toString))

      def emptyWithIdAndConfig(id: Long, config: RawChannelConfiguration) =
        RawChannel(RawChannelId(path = "", escenicId = id, label = id.toString), config = config)

      def emptyWithIdAndChildren(id: Long, children: Seq[RawChannel]) =
        RawChannel(RawChannelId(path = "", escenicId = id, label = id.toString), children = children)

      def emptyWithIdAndChildrenAndConfig(id: Long, children: Seq[RawChannel], config: RawChannelConfiguration) =
        RawChannel(RawChannelId(path = "", escenicId = id, label = id.toString), children = children, config = config)
    }


    object configuration {

      def withTitleAndAds(title: String, adsEnabled: Boolean) = RawChannelConfiguration(
        metadata = Some(RawChannelMetadata(title = Some(title))),
        commercial = RawChannelCommercial(definesAdTag = adsEnabled, definesVideoAdTag = adsEnabled)
      )

      def withAds(adsEnabled: Boolean) = RawChannelConfiguration(
        commercial = RawChannelCommercial(definesAdTag = adsEnabled, definesVideoAdTag = adsEnabled)
      )


    }

  }

}
