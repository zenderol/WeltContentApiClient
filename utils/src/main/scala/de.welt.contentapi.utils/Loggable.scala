package de.welt.contentapi.utils

import org.slf4j.{Logger, LoggerFactory}

trait Loggable {

  /**
    * The configured logger.
    * Removes some scala-compiler class names. (Anonymous classes)
    */
  implicit val log: Logger = LoggerFactory.getLogger(getClass.getName.stripSuffix("$"))

}
