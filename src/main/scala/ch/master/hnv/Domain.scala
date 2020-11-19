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

  // graph classes

  case class HnvNode(id: Long, labels: List[String], properties: Properties)

  trait Relation {
    def source: Long
    def target: Long
  }

  case class KnowsRelation(source: Long, target: Long, movieId: Long)
      extends Relation
  
  case class PlayInRelation(
      source: Long,
      target: Long,
      character: Option[String],
      order: Int
  ) extends Relation

  case class KnownForRelation(source: Long, target: Long, count: Int)
      extends Relation

  case class BelongsToRelation(source: Long, target: Long)
      extends Relation

  case class Graph(nodes: List[HnvNode], relationships: List[Relation])

}
