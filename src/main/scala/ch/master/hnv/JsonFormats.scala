package ch.master.hnv

import ch.master.hnv.Domain._
import spray.json.DefaultJsonProtocol
import spray.json.RootJsonFormat
import spray.json.JsValue

/** Formats definitions to parse JSON with spray
  */
object JsonFormats {
  import DefaultJsonProtocol._

  implicit val genreFormat = jsonFormat6(Genre)
  implicit val playInMovieFormat = jsonFormat3(PlayInMovie)
  implicit val creditFormat = jsonFormat1(Credits)
  implicit val actorFormat = jsonFormat14(Actor)
  implicit val movieFormat = jsonFormat13(Movie)

  implicit object PropertiesJsonFormat extends RootJsonFormat[Data] {
    def read(json: JsValue): Data = ???
    def write(obj: Data): JsValue = obj match {
      case a: Actor => actorFormat.write(a)
      case g: Genre => genreFormat.write(g)
      case m: Movie => movieFormat.write(m)
    }
  }

  // implicit val propertiesFormat = jsonFormat(Properties)

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
