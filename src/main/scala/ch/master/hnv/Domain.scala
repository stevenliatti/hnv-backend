package ch.master.hnv
import java.{util => ju}

import scala.collection.mutable

import org.neo4j.driver.Value
import org.neo4j.driver.types.{Node, Relationship}
import java.util.ArrayList

object Domain {
  trait Data {
    def classes = List("Data")
  }

  case class PlayInMovie(id: Long, character: Option[String], order: Long)
  case class Credits(cast: List[PlayInMovie])

  case class Actor(
      id: Long,
      tmdbId: Long,
      name: String,
      biography: Option[String],
      birthday: Option[String],
      deathday: Option[String],
      gender: String,
      place_of_birth: Option[String],
      profile_path: Option[String],
      movie_credits: Option[Credits],
      knowsDegree: Long,
      playInDegree: Long,
      degree: Long,
      knowsCommunity: Long
  ) extends Data {
    override def classes: List[String] = List("Actor")
  }

  case class ProductionCountry(id: Long, iso_3166_1: String, name: String)
      extends Data
  case class Movie(
      id: Long,
      tmdbId: Long,
      title: String,
      overview: String,
      budget: Long,
      revenue: Long,
      genres: List[Genre],
      credits: Option[Credits],
      backdrop_path: Option[String],
      poster_path: Option[String],
      production_countries: Option[List[ProductionCountry]],
      release_date: Option[String],
      runtime: Option[Long],
      tagline: Option[String]
  ) extends Data {
    override def classes: List[String] = List("Movie")
  }

  case class MovieWithActors(movie: Movie, actors: List[Actor])
  case class ActorWithRelative(
      actor: Actor,
      friends: List[Actor],
      movies: List[Movie],
      genres: List[Genre],
      playInRelations: List[PlayInRelation],
      knowsRelations: List[KnowsRelation],
      knownForRelations: List[KnownForRelation]
  )

  case class Genre(
      id: Long,
      tmdbId: Long,
      name: String,
      belongsToDegree: Long,
      knownForDegree: Long,
      degree: Long
  ) extends Data {
    override def classes: List[String] = List("Genre")
  }

  // graph classes

  case class HnvNode(data: Data)

  trait Relation {
    def source: Long
    def target: Long
  }

  case class KnowsRelation(source: Long, target: Long, movieIds: List[Long])
      extends Relation

  case class PlayInRelation(
      source: Long,
      target: Long,
      character: Option[String],
      order: Long
  ) extends Relation

  case class KnownForRelation(source: Long, target: Long, count: Long)
      extends Relation

  case class BelongsToRelation(source: Long, target: Long) extends Relation
  case class ProducedInRelation(source: Long, target: Long) extends Relation

  case class Paths(nodes: List[Node], rels: Relationship)

  case class PairIds(one: Long, another: Long) {
    val pair: Set[Long] = Set.apply(one, another)

    override def equals(obj: Any): Boolean =
      obj match {
        case that: PairIds => pair == that.pair
        case _             => false
      }
  }

  case class RelData(data: Relation)

  case class Graph(nodes: List[HnvNode], edges: List[RelData])

  // Cases classes for search
  case class Result(id: Long, name: String, lbl: List[String])
  case class ResultFormat(tmdbId: Long, name: String, label: String)

}
