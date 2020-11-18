package ch.master.hnv
import java.{util => ju}

import scala.collection.mutable

import org.neo4j.driver.Value
import org.neo4j.driver.types.{Node, Relationship}
import java.util.ArrayList

object Domain {
  trait Properties

  case class PlayInMovie(id: Long, character: Option[String], order: Int)
  case class Credits(cast: List[PlayInMovie])

  case class Actor(
      id: Long,
      name: String,
      biography: Option[String],
      birthday: Option[String],
      deathday: Option[String],
      gender: String,
      place_of_birth: Option[String],
      profile_path: Option[String],
      movie_credits: Option[Credits]
  ) extends Properties

  case class Movie(
      id: Long,
      title: String,
      overview: String,
      budget: Long,
      revenue: Long,
      genres: List[Genre],
      credits: Option[Credits],
      backdrop_path: Option[String],
      poster_path: Option[String],
      release_date: Option[String],
      runtime: Option[Int],
      tagline: Option[String]
  ) extends Properties

  case class Genre(id: Long, name: String) extends Properties

  case class NodeA(id: Long, label: String)
  case class Relation(source: Long, target: Long)
  case class Graph(nodes: List[NodeA], relationships: List[Relation])

  trait Node extends org.neo4j.driver.types.Node {
    def id: Long
    def label: String
    def properties: Properties

    override def labels(): java.lang.Iterable[String] = {
      val list = new ArrayList[String]
      list.add(label)
      list
    }
    override def hasLabel(x: String): Boolean = label == x
  }

  trait Relationship extends org.neo4j.driver.types.Relationship {
    override def keys(): java.lang.Iterable[String] = ???
    override def containsKey(x: String): Boolean = ???
    override def get(x: String): Value = ???
    override def size(): Int = ???
    override def values(): java.lang.Iterable[Value] = ???
    override def values[T <: Object](
        x: java.util.function.Function[Value, T]
    ): java.lang.Iterable[T] = ???
    override def asMap(): ju.Map[String, Object] = ???
    override def asMap[T <: Object](
        x: java.util.function.Function[Value, T]
    ): ju.Map[String, T] = ???
    override def id(): Long = ???

    def source: Long
    def target: Long
    override def startNodeId(): Long = source
    override def endNodeId(): Long = target
    override def hasType(x: String): Boolean = x == `type`
  }

  case class KnowsRelationship(source: Long, target: Long, movieId: Long)
      extends Relationship {
    override def `type`(): String = "KNOWS"
  }

  case class PlayInRelationship(
      source: Long,
      target: Long,
      character: Option[String],
      order: Int
  ) extends Relationship {
    override def `type`(): String = "PLAY_IN"
  }

  case class KnownForRelationship(source: Long, target: Long, count: Int)
      extends Relationship {
    override def `type`(): String = "KNOWN_FOR"
  }

  case class BelongsToRelationship(source: Long, target: Long)
      extends Relationship {
    override def `type`(): String = "BELONGS_TO"
  }

}
