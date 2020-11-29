package ch.master.hnv

import ch.master.hnv.Domain._
import spray.json.DefaultJsonProtocol
import spray.json.RootJsonFormat
import spray.json.JsValue
import spray.json.JsArray

/** Formats definitions to parse JSON with spray
  */
object JsonFormats {
  import DefaultJsonProtocol._

  case class Empty(message: String)
  implicit val emptyFormat = jsonFormat1(Empty)

  implicit val genreFormat = jsonFormat6(Genre)
  implicit val playInMovieFormat = jsonFormat3(PlayInMovie)
  implicit val creditFormat = jsonFormat1(Credits)
  implicit val productionCoutriesFormat = jsonFormat2(ProductionCountries)
  implicit val actorFormat = jsonFormat14(Actor)
  implicit val movieFormat = jsonFormat14(Movie)
  implicit val resultFormatFormat = jsonFormat3(ResultFormat)
  implicit val searchResultsFormat = jsonFormat1(SearchResults)

  implicit object MoviesJsonFormat extends RootJsonFormat[List[Movie]] {
    override def read(json: JsValue): List[Domain.Movie] = ???
    override def write(obj: List[Domain.Movie]): JsValue = JsArray(
      obj.map(m => movieFormat.write(m))
    )
  }

  implicit object PropertiesJsonFormat extends RootJsonFormat[Data] {
    def read(json: JsValue): Data = ???
    def write(obj: Data): JsValue = obj match {
      case a: Actor => actorFormat.write(a)
      case g: Genre => genreFormat.write(g)
      case m: Movie => movieFormat.write(m)
    }
  }

  implicit val hnvNodeFormat = jsonFormat1(HnvNode)

  implicit val knowsRelationFormat = jsonFormat3(KnowsRelation)
  implicit val playInRelationFormat = jsonFormat4(PlayInRelation)
  implicit val knownForRelationFormat = jsonFormat3(KnownForRelation)
  implicit val belongsToRelationFormat = jsonFormat2(BelongsToRelation)

  implicit object RelationJsonFormat extends RootJsonFormat[Relation] {
    def read(json: JsValue): Relation = ???
    def write(obj: Relation): JsValue = obj match {
      case a: BelongsToRelation => belongsToRelationFormat.write(a)
      case b: KnownForRelation  => knownForRelationFormat.write(b)
      case c: KnowsRelation     => knowsRelationFormat.write(c)
      case d: PlayInRelation    => playInRelationFormat.write(d)
    }
  }

  implicit val relDataFormat = jsonFormat1(RelData)

  implicit val graphFormat = jsonFormat2(Graph)
}
