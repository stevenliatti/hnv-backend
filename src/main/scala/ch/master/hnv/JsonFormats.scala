package ch.master.hnv

import ch.master.hnv.Domain._
import spray.json.DefaultJsonProtocol

/**
  * Formats definitions to parse JSON with spray
  */
object JsonFormats {
  import DefaultJsonProtocol._

  implicit val genreFormat = jsonFormat2(Genre)
  implicit val playInMovieFormat = jsonFormat3(PlayInMovie)
  implicit val creditFormat = jsonFormat1(Credits)
  implicit val actorFormat = jsonFormat9(Actor)
  implicit val movieFormat = jsonFormat12(Movie)
}
