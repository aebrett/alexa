package org.bretts.alexa.fembot

import com.amazon.speech.speechlet._
import com.amazon.speech.ui.{PlainTextOutputSpeech, Reprompt}
import com.typesafe.scalalogging.StrictLogging
import org.bretts.alexa.util._

object FembotSpeechlet extends StrictLogging {

  private def logInvocation(name: String, request: SpeechletRequest, session: Session): Unit = {
    val requestId = request.getRequestId
    val sessionId = session.getSessionId
    request match {
      case ir: IntentRequest =>
        val intent = ir.intent
        val continuationIntent = session.continuation
        val slots = ir.slots.map { case (n, s) =>
          s"$n -> $s"
        }.mkString("[", ", ", "]")
        val attributes = session.attributes.map { case (n, a) =>
          s"$n -> $a"
        }.mkString("[", ", ", "]")
        logger.info(
          s"""FembotSpeechlet.$name:
             |  requestId: $requestId
             |  sessionId:$sessionId
             |  intent: $intent
             |  slots: $slots
             |  continuation: $continuationIntent
             |  attributes: $attributes""".stripMargin
        )
      case _ =>
        logger.info(s"FembotSpeechlet.$name requestId=$requestId sessionId=$sessionId")
    }

  }

  private def str(sr: SpeechletResponse): String = sr.getOutputSpeech match {
    case os: PlainTextOutputSpeech => os.getText
    case _ => sr.toString
  }
}

class FembotSpeechlet extends Speechlet with StrictLogging {
  import FembotSpeechlet._

  override def onSessionStarted(request: SessionStartedRequest, session: Session): Unit = {
    logInvocation("onSessionStarted", request, session)
  }

  override def onLaunch(request: LaunchRequest, session: Session): SpeechletResponse = {
    logInvocation("onLaunch", request, session)

    val outputSpeech = new PlainTextOutputSpeech
    outputSpeech.setText("How can I help you? For example, you can say \"Run a test\"")

    val reprompt = new Reprompt
    reprompt.setOutputSpeech(outputSpeech)
    SpeechletResponse.newAskResponse(outputSpeech, reprompt)
  }

  private lazy val cp = new CouchPotatoSpeechModel(sys.env("CP_URL"), sys.env("CP_API_KEY"))

  override def onIntent(request: IntentRequest, session: Session): SpeechletResponse = {
    logInvocation("onIntent", request, session)

    val coreIntent = session.continuation.getOrElse(request.intent)

    val response: SpeechletResponse = coreIntent match {
      case "Test" =>
        reply("Foo, Bar")
      case CouchPotatoSpeechModel.Intent(_) =>
        cp.handle(request, session)
      case intent =>
        reply(s"Sorry, I didn't understand the intent $intent")
    }
    logger.info(s"Sending response: ${str(response)}")
    response
  }

  override def onSessionEnded(request: SessionEndedRequest, session: Session): Unit = {
    logInvocation("onSessionEnded", request, session)
  }
}