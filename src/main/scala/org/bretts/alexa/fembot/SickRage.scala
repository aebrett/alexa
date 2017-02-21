package org.bretts.alexa.fembot


import java.net.URLEncoder
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
import spray.json._

import scala.concurrent.{Await, Future}
import scala.util.matching.Regex

object SickRage {
  private val outputDayFormat = DateTimeFormatter.ofPattern("EEEE")
  private val outputDateFormat = DateTimeFormatter.ofPattern("d LLL")

  case class Show(show_name: String, airs: String) {
    def toSpokenString: String = s"$show_name is on ${airs.split(" ").patch(1, Seq("at"), 0).mkString(" ")}"
  }
  case class FutureData(today: Seq[Show], soon: Seq[Show])
  case class FutureResponse(data: FutureData)

  case class HistoryShow(showName: String, status: String, date: LocalDateTime, season: Int, episode: Int) {

    def toSpokenString: String = {
      val dateStr =
        if (date.toLocalDate == LocalDate.now) "today"
        else if (DAYS.between(date.toLocalDate, LocalDate.now) < 7) s"on ${outputDayFormat.format(date)}"
        else s"on ${outputDateFormat.format(date)}"

      s"$showName was ${status.toLowerCase} $dateStr"
    }
  }
  case class HistoryResponse(data: Seq[HistoryShow])

  case class SearchShow(name: String, tvdbid: String, firstAired: LocalDate)
  case class SearchData(results: Option[Seq[SearchShow]])
  case class SearchResponse(data: Option[SearchData])

  case class SimpleResponse(result: Option[String]) {
    def success: Boolean = result.contains("success")
  }
}
import SickRage._

object SickRageJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val showFormat: RootJsonFormat[Show] = jsonFormat2(Show)
  implicit val futureDataFormat: RootJsonFormat[FutureData] = jsonFormat2(FutureData)
  implicit val futureResponseFormat: RootJsonFormat[FutureResponse] = jsonFormat1(FutureResponse)

  implicit object LocalDateTimeJsonFormat extends JsonFormat[LocalDateTime] {
    private val inputDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm[:ss]")

    def write(dt: LocalDateTime) = JsString(inputDateFormat.format(dt))

    def read(value: JsValue): LocalDateTime = value match {
      case JsString(s) => inputDateFormat.parse(s, LocalDateTime.from _)
      case _ => deserializationError(s"Date time in the format yyyy-MM-dd HH:mm[:ss] expected, but got $value")
    }
  }

  implicit val historyShowFormat: RootJsonFormat[HistoryShow] =
    jsonFormat(HistoryShow, "show_name", "status", "date", "season", "episode")
  implicit val historyResponseFormat: RootJsonFormat[HistoryResponse] = jsonFormat1(HistoryResponse)

  implicit object LocalDateJsonFormat extends JsonFormat[LocalDate] {

    def write(dt: LocalDate) = JsString(dt.toString)

    def read(value: JsValue): LocalDate = value match {
      case JsString(s) => LocalDate.parse(s)
      case _ => deserializationError(s"Date in the format yyyy-MM-dd expected, but got $value")
    }
  }

  implicit val searchShowFormat: RootJsonFormat[SearchShow] = jsonFormat(SearchShow, "name", "tvdbid", "first_aired")
  implicit val searchDataFormat: RootJsonFormat[SearchData] = jsonFormat1(SearchData)
  implicit val searchResponseFormat: RootJsonFormat[SearchResponse] = jsonFormat1(SearchResponse)

  implicit val simpleResponseFormat: RootJsonFormat[SimpleResponse] = jsonFormat1(SimpleResponse)
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
    val requestUrl = s"$url/api/$apiKey/?cmd=history&limit=10"
    logger.info(s"history URL: $requestUrl")
    val response: Future[HistoryResponse] = Http().singleRequest(HttpRequest(
      uri = requestUrl
    )).flatMap(Unmarshal(_).to[HistoryResponse])

    response.map(_.data).map { s =>
      s.groupBy(h => (h.showName, h.season, h.episode))
        .mapValues(_.sortWith(_.date isAfter _.date).head)
        .values.toList.sortWith(_.date isAfter _.date).take(5)
    }
  }

  def searchProviders(search: String): Future[Seq[SearchShow]] = {
    val requestUrl = s"$url/api/$apiKey/?cmd=sb.searchtvdb&name=${URLEncoder.encode(search, "UTF-8")}&lang=en"
    logger.info(s"searchProviders URL: $requestUrl")
    val response: Future[SearchResponse] = Http().singleRequest(HttpRequest(
      uri = requestUrl
    )).flatMap(Unmarshal(_).to[SearchResponse])

    response.map(_.data.flatMap(_.results).getOrElse(Seq()))
  }

  def addShow(tvdbid: String): Future[Boolean] = {
    val requestUrl = s"$url/api/$apiKey/?cmd=show.addnew&tvdbid=$tvdbid&lang=en&status=wanted"
    logger.info(s"addMovie URL: $requestUrl")
    val response: Future[SimpleResponse] = Http().singleRequest(HttpRequest(
      uri = requestUrl
    )).flatMap(Unmarshal(_).to[SimpleResponse])

    response.map(_.success)
  }

}

object SickRageSpeechModel {
  val Intent: Regex = "SickRage.*".r
}

class SickRageSpeechModel(url: String, apiKey: String) extends StrictLogging {
  import SickRageJsonSupport._

  val sr = new SickRage(url, apiKey)

  def handle(request: IntentRequest, session: Session): SpeechletResponse = session.continuation match {
    case None => request.intent match {
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
      case "SickRageAdd" =>
        val title = request.slotOption("show")
        title.map { t =>
          Await.result(sr.searchProviders(t), timeout) match {
            case head +: tail =>
              requestAddResponse(t, head, tail, session)
            case Seq() =>
              reply(s"Sorry, I couldn't find any shows named '$t'")
          }
        }.getOrElse(reply("Please specify a show name"))
      case intent =>
        reply(s"Sorry, I didn't understand the intent '$intent'")
    }
    case Some("SickRageAdd") =>
      val show = session.attribute[SearchShow]("show")
      request.intent match {
        case "AMAZON.YesIntent" =>
          if (Await.result(sr.addShow(show.tvdbid), timeout))
            reply(s"'${show.name}' successfully added")
          else
            reply(s"Sorry, '${show.name}' could not be added")
        case "AMAZON.NoIntent" =>
          session.attribute[Seq[SearchShow]]("remaining") match {
            case head +: tail =>
              requestAddResponse(session.attribute[String]("name"), head, tail, session)
            case Seq() =>
              val name = session.attribute[String]("name")
              reply(s"No more movies found named '$name'")
          }
        case "AMAZON.StopIntent" =>
          reply("OK, stopped")
        case intent =>
          ask(s"Sorry, I didn't understand the intent '$intent'")
      }
    case Some(c) =>
      reply(s"Sorry, I didn't understand the continuation '$c'")
  }

  private def requestAddResponse(
    name: String, show: SearchShow, remaining: Seq[SearchShow], session: Session
  ): SpeechletResponse = {
    session.continuation = "SickRageAdd"
    session.attribute_=("name", name)
    session.attribute_=("show", show)
    session.attribute_=("remaining", remaining)
    val yearText = s", first aired in ${show.firstAired.getYear}"
    val responseText = s"Did you mean '${show.name}'$yearText? You can answer 'Yes', 'No', or 'Stop'."
    ask(responseText)
  }

}
