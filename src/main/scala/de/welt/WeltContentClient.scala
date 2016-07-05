package de.welt

trait WeltContentClientLogic {

  val username: String
  val password: String

}

class WeltContentClient(val username: String, val password: String) extends WeltContentClientLogic
