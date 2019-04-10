package de.welt.contentapi.utils

import play.api.Logger

trait Loggable {
  implicit val log: Logger = Logger(getClass.getName.stripSuffix("$"))
}
