package org.bretts.alexa.fembot

import com.amazon.speech.speechlet.lambda.SpeechletRequestStreamHandler

import scala.collection.JavaConverters._

class FembotHandler extends SpeechletRequestStreamHandler(new FembotSpeechlet, Set[String]().asJava)