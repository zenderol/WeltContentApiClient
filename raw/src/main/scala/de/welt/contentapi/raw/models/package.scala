package de.welt.contentapi.raw

import de.welt.contentapi.utils.Loggable

package object models extends Loggable {

  /**
    * collect changes between two [[RawChannel]]
    *
    * @param added   stuff that has been added to the tree
    * @param deleted stuff that has been removed from the tree
    * @param moved   stuff that moved within the tree
    */
  case class ChannelUpdate(added: Seq[RawChannel] = Seq.empty, deleted: Seq[RawChannel] = Seq.empty, moved: Seq[RawChannel] = Seq.empty) {

    def merge(other: ChannelUpdate): ChannelUpdate = ChannelUpdate(
      added = (added ++ other.added).distinct,
      deleted = (deleted ++ other.deleted).distinct,
      moved = (moved ++ other.moved).distinct
    )

    def isEmpty: Boolean = added.isEmpty && deleted.isEmpty && moved.isEmpty

    /** merge all the updates into this */
    def merge(updates: Seq[ChannelUpdate]): ChannelUpdate = updates.foldLeft(this)((acc, update) ⇒ acc.merge(update))
  }


}
