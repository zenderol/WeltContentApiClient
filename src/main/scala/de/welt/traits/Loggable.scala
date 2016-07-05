package de.welt.traits

import org.slf4j.LoggerFactory


trait Loggable {

  implicit val log = LoggerFactory.getLogger(getClass.getName.stripSuffix("$"))

}
