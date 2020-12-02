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

  object ActorsJsonFormat extends RootJsonFormat[List[Actor]] {
    override def read(json: JsValue): List[Actor] = ???
    override def write(obj: List[Actor]): JsValue = JsArray(
      obj.map(m => actorFormat.write(m))
    )
  }

  implicit object MoviesJsonFormat extends RootJsonFormat[List[Movie]] {
    override def read(json: JsValue): List[Movie] = ???
    override def write(obj: List[Movie]): JsValue = JsArray(
      obj.map(m => movieFormat.write(m))
    )
  }

  implicit val movieWithActorsFormat = jsonFormat2(MovieWithActors)

  implicit object MoviesWithActorsJsonFormat
      extends RootJsonFormat[List[MovieWithActors]] {
    override def read(json: JsValue): List[MovieWithActors] = ???
    override def write(obj: List[MovieWithActors]): JsValue = JsArray(
      obj.map(m => movieWithActorsFormat.write(m))
    )
  }

  implicit object SearchResultsFormat
      extends RootJsonFormat[List[ResultFormat]] {
    override def read(json: JsValue): List[Domain.ResultFormat] = ???
    override def write(obj: List[Domain.ResultFormat]): JsValue = JsArray(
      obj.map(m => resultFormatFormat.write(m))
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
