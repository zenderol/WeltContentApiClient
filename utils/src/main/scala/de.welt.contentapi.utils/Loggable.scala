package de.welt.contentapi.utils

import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

trait Loggable {

  /**
    * The configured logger.
    * Removes some scala-compiler class names. (Anonymous classes)
    */
  implicit val log: Logger = Logger(LoggerFactory.getLogger(getClass.getName.stripSuffix("$")))

}
