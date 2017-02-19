package org.bretts.alexa.fembot

import org.bretts.alexa.util._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

object CouchPotatoTest extends App {
  try {
    val r = new CouchPotato(sys.env("CP_URL"), sys.env("CP_API_KEY")).searchProviders("rogue one")
    println(Await.result(r, 20 seconds))
  } finally {
    system.terminate()
  }
}
