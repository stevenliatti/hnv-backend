package ch.master.hnv

import org.neo4j.driver.AuthTokens
import neotypes.GraphDatabase
import neotypes.implicits.all._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import neotypes.Driver
import spray.json.JsonParser

import scala.collection.mutable
import scala.concurrent.Future
import scala.io.Source

import ch.master.hnv.Domain._
import ch.master.hnv.MovieService.MovieToActor

class DataService(host: String, user: String, password: String) {
  private val driver =
    GraphDatabase.driver[Future](host, AuthTokens.basic(user, password))
  // private val session = driver.session

  // val people = "match (p:Person) return p.name, p.born limit 10".query[(String, Int)].list(session)

  def hello = "hello"

  /** Return actors list with KNOWS relationship
    *
    * @param nb
    * @param filter
    * @param sort
    * @param movies
    * @return
    */
  def actors(
      nb: Int,
      filter: String,
      sort: String,
      movies: List[Long]
  ): List[Actor] = ???

  // TODO inspired by https://github.com/neotypes/examples
  def graph(limit: Int) = driver.readSession { session =>
    c"""MATCH (m:Movie)<-[:ACTED_IN]-(a:Person)
    RETURN m.title as movie, collect(a.name) as cast
    LIMIT $limit"""
      .query[Graph]
      .list(session)
      // .map { result =>
      //   val nodes = result.flatMap(toNodes)

      //   val map = nodes.zipWithIndex.toMap
      //   val rels = result.flatMap { mta =>
      //     val movieIndex = map(Node(mta.movie, MovieService.LABEL_MOVIE))
      //     mta.cast
      //       .map(c => Relation(movieIndex, map(Node(c, MovieService.LABEL_ACTOR))))
      //   }

      //   Graph(nodes, rels)
      // }
  }

  // private[this] def toNodes(movieToActor: MovieToActor): Seq[Node] =
  //   movieToActor.cast.map(c => Node(c, MovieService.LABEL_ACTOR)) :+ Node(
  //     movieToActor.movie,
  //     MovieService.LABEL_MOVIE
  //   )

}

object MovieService {
  val LABEL_ACTOR = "actor"
  val LABEL_MOVIE = "movie"

  case class MovieToActor(movie: String, cast: List[String])

}
