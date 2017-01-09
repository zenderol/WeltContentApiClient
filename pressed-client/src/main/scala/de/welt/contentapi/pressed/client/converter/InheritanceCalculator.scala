package de.welt.contentapi.pressed.client.converter

import javax.inject.Inject

import de.welt.contentapi.raw.models.RawChannel

class InheritanceCalculator @Inject()() {

  /**
    * Generic Top-Down Inheritance.
    * if predicate matches -> all subchannels may inherit from this channel
    *
    * {{{
    * root-channel
    * ├── channel-A (*channel with matching predicate)
    * │   ├── sub-channel-A-1
    * │   ├── sub-channel-A-2
    * │   │   ├── sub-channel-A-2-1 (*channel to check)
    * │   │   └── sub-channel-A-2-2
    * │   └── sub-channel-A-3
    * └── channel-B
    *
    * Result: `sub-channel-A-2-1` will inherit from `channel-A`
    * }}}
    *
    * @param rawChannel calculate inherited value(s) for this channel with its ancestors
    * @param action     all possible actions for inheritance. See [[InheritanceAction]]
    * @param predicate  fn to check if a channel may inherit its data
    */
  def forChannel[T](rawChannel: RawChannel, action: InheritanceAction[T], predicate: RawChannel ⇒ Boolean): T =
    rawChannel.parent match {
      case None ⇒
        // is root -> result for root
        action.forRoot.apply(rawChannel)

      case Some(_) if predicate.apply(rawChannel) ⇒
        // happy inheritance path
        action.forMatching.apply(rawChannel)

      case Some(parent) if parent == rawChannel.root && !predicate.apply(rawChannel) ⇒
        // parent is root and value of current channel may not be inherited, so don't go up -> use fallback
        action.forFallback.apply(rawChannel)

      case Some(parent) ⇒
        // enter recursion
        forChannel(parent, action, predicate)
    }
}

/**
  * Holder object for the possible values for inheritance
  *
  * @param forRoot     value or function to get a result for the root channel (frontpage is mostly unique)
  * @param forFallback if no ancestor is found set this fallback value (apply fn). BEWARE: never inherit root channels values
  * @param forMatching this is the happy path for inheritance - a static value or an apply function can be provided
  */
case class InheritanceAction[T](forRoot: RawChannel ⇒ T, forFallback: RawChannel ⇒ T, forMatching: RawChannel ⇒ T)


