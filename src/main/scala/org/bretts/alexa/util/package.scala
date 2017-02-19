package org.bretts.alexa

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.amazon.speech.speechlet.{IntentRequest, Session, SpeechletResponse}
import com.amazon.speech.ui.{OutputSpeech, PlainTextOutputSpeech, Reprompt}
import com.typesafe.scalalogging.StrictLogging
import spray.json.JsonFormat

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps

package object util extends StrictLogging {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = system.dispatcher

  val timeout: FiniteDuration = 10 seconds

  def output(text: String): PlainTextOutputSpeech = {
    val o = new PlainTextOutputSpeech
    o.setText(text)
    o
  }

  def reprompt(output: OutputSpeech): Reprompt = {
    val r = new Reprompt
    r.setOutputSpeech(output)
    r
  }

  def reprompt(text: String): Reprompt = reprompt(output(text))

  def reply(text: String): SpeechletResponse = SpeechletResponse.newTellResponse(output(text))

  def ask(text: String): SpeechletResponse = SpeechletResponse.newAskResponse(output(text), reprompt(text))

  implicit class IntentRequestOps(val request: IntentRequest) extends AnyVal {
    def intent: String = request.getIntent.getName
    def slots: Map[String, String] = request.getIntent.getSlots.asScala.mapValues(_.getValue).toMap
    def slot(name: String): String =
      slotOption(name).getOrElse(throw new IllegalStateException(s"No slot found for key: $name"))
    def slotOption(name: String): Option[String] =
      Option(request.getIntent.getSlot(name)).flatMap(s => Option(s.getValue))
  }

  implicit class SessionOps(val session: Session) extends AnyVal {
    import spray.json._

    def continuation: Option[String] = Option(session.getAttribute("continuation").asInstanceOf[String])
    def continuation_=(intent: String): Unit = session.setAttribute("continuation", intent)

    def attributes: Map[String, AnyRef] = session.getAttributes.asScala.toMap

    def attribute[A](name: String)(implicit f: JsonFormat[A]): A =
      attributeOpt[A](name).getOrElse(throw new IllegalStateException(s"No attribute found for key: $name"))
    def attributeOpt[A](name: String)(implicit f: JsonFormat[A]): Option[A] =
      Option(session.getAttribute(name).asInstanceOf[String]).map { s =>
        s.parseJson.convertTo[A]
      }

    def attribute_=[A](name: String, a: A)(implicit f: JsonFormat[A]): Unit = {
      val serialized = a.toJson.compactPrint
      session.setAttribute(name, serialized)
    }
  }


}
