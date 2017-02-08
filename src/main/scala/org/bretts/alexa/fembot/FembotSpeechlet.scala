package org.bretts.alexa.fembot

import com.amazon.speech.speechlet._
import com.amazon.speech.ui.{PlainTextOutputSpeech, Reprompt}
import com.typesafe.scalalogging.StrictLogging

object FembotSpeechlet extends StrictLogging {

  private def logInvocation(name: String, request: SpeechletRequest, session: Session): Unit = {
    val requestId = request.getRequestId
    val sessionId = session.getSessionId
    logger.info(s"$name requestId=$requestId sessionId=$sessionId")
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

  override def onIntent(request: IntentRequest, session: Session): SpeechletResponse = {
    logInvocation("onIntent", request, session)

    val responseText = request.getIntent.getName match {
      case "Test" =>
        "Foo, Bar"
      case intent =>
        s"Sorry, I didn't understand the intent $intent"
    }
    val output = new PlainTextOutputSpeech
    output.setText(responseText)
    logger.info(s"Sending response: $responseText")
    SpeechletResponse.newTellResponse(output)
  }

  override def onSessionEnded(request: SessionEndedRequest, session: Session): Unit = {
    logInvocation("onSessionEnded", request, session)
  }
}