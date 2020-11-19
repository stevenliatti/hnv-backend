package ch.master.hnv

import ch.master.hnv.Domain._
import spray.json.DefaultJsonProtocol
import spray.json.RootJsonFormat
import spray.json.JsValue

/** Formats definitions to parse JSON with spray
  */
object JsonFormats {
  import DefaultJsonProtocol._

  implicit val genreFormat = jsonFormat2(Genre)
  implicit val playInMovieFormat = jsonFormat3(PlayInMovie)
  implicit val creditFormat = jsonFormat1(Credits)
  implicit val actorFormat = jsonFormat9(Actor)
  implicit val movieFormat = jsonFormat12(Movie)

  implicit object PropertiesJsonFormat extends RootJsonFormat[Properties] {
    def read(json: JsValue): Domain.Properties = ???
    def write(obj: Domain.Properties): JsValue = obj match {
      case a: Actor => actorFormat.write(a)
      case g: Genre => genreFormat.write(g)
      case m: Movie => movieFormat.write(m)
    }
  }

  // implicit val propertiesFormat = jsonFormat(Properties)

  implicit val hnvNodeFormat = jsonFormat3(HnvNode)

  implicit val knowsRelationFormat = jsonFormat3(KnowsRelation)
  implicit val playInRelationFormat = jsonFormat4(PlayInRelation)
  implicit val knownForRelationFormat = jsonFormat3(KnownForRelation)
  implicit val belongsToRelationFormat = jsonFormat2(BelongsToRelation)
  
  implicit object RelationJsonFormat extends RootJsonFormat[Relation] {
    def read(json: JsValue): Domain.Relation = ???
    def write(obj: Domain.Relation): JsValue = obj match {
      case a: BelongsToRelation => belongsToRelationFormat.write(a)
      case b: KnownForRelation => knownForRelationFormat.write(b)
      case c: KnowsRelation => knowsRelationFormat.write(c)
      case d: PlayInRelation => playInRelationFormat.write(d)
    }
  }
  
  implicit val graphFormat = jsonFormat2(Graph)
}
