package org.bretts.alexa.fembot


import java.time.{LocalDate, LocalDateTime}
import java.time.temporal.ChronoUnit._
import java.time.format.DateTimeFormatter

import akka.http.scaladsl._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.amazon.speech.speechlet.{IntentRequest, Session, SpeechletResponse}
import com.typesafe.scalalogging.StrictLogging
import org.bretts.alexa.util._
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

import scala.concurrent.{Await, Future}
import scala.util.matching.Regex

case class Show(show_name: String, airs: String) {
  def toSpokenString: String = s"$show_name is on ${airs.split(" ").patch(1, Seq("at"), 0).mkString(" ")}"
}
case class FutureData(today: Seq[Show], soon: Seq[Show])
case class FutureResponse(data: FutureData)

case class HistoryShow(show_name: String, status: String, date: String) {
  private def dateTime = SickRage.inputDateFormat.parse[LocalDateTime](date, LocalDateTime.from _)

  def toSpokenString: String = {
    val dateStr =
      if (DAYS.between(dateTime.toLocalDate, LocalDate.now) < 7) SickRage.outputDayFormat.format(dateTime)
      else SickRage.outputDateFormat.format(dateTime)

    s"$show_name was ${status.toLowerCase} on $dateStr"
  }
}
case class HistoryResponse(data: Seq[HistoryShow])

object SickRageJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val showFormat: RootJsonFormat[Show] = jsonFormat2(Show)
  implicit val futureDataFormat: RootJsonFormat[FutureData] = jsonFormat2(FutureData)
  implicit val futureResponseFormat: RootJsonFormat[FutureResponse] = jsonFormat1(FutureResponse)

  implicit val historyShowFormat: RootJsonFormat[HistoryShow] = jsonFormat3(HistoryShow)
  implicit val historyResponseFormat: RootJsonFormat[HistoryResponse] = jsonFormat1(HistoryResponse)
}

object SickRage {
  val inputDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm[:ss]")
  val outputDayFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE")
  val outputDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d LLL")
}

class SickRage(url: String, apiKey: String) extends StrictLogging {
  import SickRageJsonSupport._

  def futureShows: Future[Seq[Show]] = {
    val requestUrl = s"$url/api/$apiKey/?cmd=future&type=today|soon"
    logger.info(s"futureShows URL: $requestUrl")
    val response: Future[FutureResponse] = Http().singleRequest(HttpRequest(
      uri = requestUrl
    )).flatMap(Unmarshal(_).to[FutureResponse])

    response.map(r => r.data.today ++ r.data.soon)
  }

  def history: Future[Seq[HistoryShow]] = {
    val requestUrl = s"$url/api/$apiKey/?cmd=history&limit=3"
    logger.info(s"history URL: $requestUrl")
    val response: Future[HistoryResponse] = Http().singleRequest(HttpRequest(
      uri = requestUrl
    )).flatMap(Unmarshal(_).to[HistoryResponse])

    response.map(_.data)
  }

}

object SickRageSpeechModel {
  val Intent: Regex = "SickRage.*".r
}

class SickRageSpeechModel(url: String, apiKey: String) extends StrictLogging {

  val sr = new SickRage(url, apiKey)

  def handle(request: IntentRequest, session: Session): SpeechletResponse = request.intent match {
    case "SickRageFuture" =>
      val shows = Await.result(sr.futureShows, timeout)
      if (shows.isEmpty) {
        reply("No shows are on in the next week")
      } else {
        reply(shows.map(_.toSpokenString).mkString(", "))
      }
    case "SickRageHistory" =>
      val shows = Await.result(sr.history, timeout)
      if (shows.isEmpty) {
        reply("No shows have been recently downloaded")
      } else {
        reply(shows.map(_.toSpokenString).mkString(", "))
      }
    case intent =>
      reply(s"Sorry, I didn't understand the intent $intent")
  }

}