package de.welt.contentapi.utils

/**
  * The env is needed to store files (json) in separated folders:
  * /${bucket}/${app}/${env}/${file}.json
  */
object Env {
  sealed trait Env

  /**
    * Preview env. For a preview app to show changes before going live.
    */
  case object Preview extends Env

  /**
    * Live env. The current live version of the file. The truth.
    */
  case object Live extends Env

  /**
    * No env provided. May be used by system tasks.
    */
  case object UndefinedEnv extends Env

  def apply(env: String): Env = env match {
    case "preview" ⇒ Preview
    case "live" ⇒ Live
    case _ ⇒ throw new IllegalArgumentException(s"Not a valid env: $env. Allowed values are 'preview' and 'live'")
  }
}
