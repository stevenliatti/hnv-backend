package ch.master.hnv

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.io.Source
import scala.jdk.CollectionConverters._

import ch.master.hnv.Domain._
import neotypes.Driver
import neotypes.implicits.mappers.all._
import neotypes.implicits.syntax.string._
import neotypes.implicits.syntax.cypher._
import neotypes.GraphDatabase
import org.neo4j.driver.AuthTokens
import org.neo4j.driver.types.Node
import org.neo4j.driver.types.Relationship
import spray.json.JsonParser

class DataService(host: String) {
  private val driver = GraphDatabase.driver[Future](host)

  def hello = "hello"

  private def nodeToActor(node: Node): Actor = {
    val nm = node.asMap
    Actor(
      node.id,
      nm.get("tmdbId").asInstanceOf[Long],
      nm.get("name").asInstanceOf[String],
      Some(nm.get("biography").asInstanceOf[String]),
      Some(nm.get("birthday").asInstanceOf[String]),
      Some(nm.get("deathday").asInstanceOf[String]),
      nm.get("gender").asInstanceOf[String],
      Some(nm.get("place_of_birth").asInstanceOf[String]),
      Some(nm.get("profile_path").asInstanceOf[String]),
      None,
      nm.get("knowsDegree").asInstanceOf[Long],
      nm.get("playInDegree").asInstanceOf[Long],
      nm.get("degree").asInstanceOf[Long],
      // TODO try with other communities if needed
      nm.get("knowsCommunityLouvain").asInstanceOf[Long]
    )
  }

  private def actorsPathsToGraph(fPaths: Future[List[Paths]]) = {
    val paths = Await.result(fPaths, Duration.Inf)

    val neo4jNodes = mutable.Set[Node]()
    val neo4jRels = mutable.Map[PairIds, List[Long]]()

    paths.foreach(path => {
      neo4jNodes.addAll(path.nodes)
      val rel = path.rels
      val pairIds = PairIds(rel.startNodeId, rel.endNodeId)
      val movieId = rel.asMap.get("movieId").asInstanceOf[Long]
      if (neo4jRels.contains(pairIds)) {
        neo4jRels.put(
          pairIds,
          movieId :: neo4jRels(pairIds)
        )
      } else {
        neo4jRels.put(pairIds, List(movieId))
      }
    })

    val nodes = neo4jNodes
      .map(node => HnvNode(nodeToActor(node)))
      .toList

    val relationships = neo4jRels.map { case (pairIds, list) =>
      RelData(KnowsRelation(pairIds.one, pairIds.another, list))
    }.toList

    Graph(nodes, relationships)
  }

  def actors(
      limitMovie: Int,
      limitActor: Int,
      limitActorFriends: Int
  ): Graph = {

    val lm = if (limitMovie > 20) 20 else limitMovie
    val la = if (limitActor > 20) 20 else limitActor
    val laf = if (limitActorFriends > 10) 10 else limitActorFriends

    def actorsQuery: Future[List[Paths]] = driver.readSession { session =>
      c"""
        MATCH (m:Movie)
        WITH m ORDER BY m.revenue DESC LIMIT $limitMovie
        CALL {
          WITH m MATCH (m)<-[p]-(a:Actor)
          RETURN a ORDER BY p.order LIMIT $limitActor
        }
        CALL {
          WITH a MATCH (a)-[k:KNOWS]-(b)
          RETURN k,b ORDER BY b.degree DESC LIMIT $limitActorFriends
        }
        WITH (collect(a.tmdbId) + collect(b.tmdbId)) AS actorIds
        MATCH (c:Actor)-[k2:KNOWS]-(d)
        WHERE c.tmdbId IN actorIds AND d.tmdbId IN actorIds
        RETURN collect(c) AS nodes, k2 AS rels
      """
        .query[Paths]
        .list(session)
    }

    actorsPathsToGraph(actorsQuery)
  }

  def friendsOf(actorId: Long, friends: Int, friendsOfFriends: Int) = {

    val f = if (friends > 30) 30 else friends
    val ff = if (friendsOfFriends > 15) 15 else friendsOfFriends

    def friendsQuery: Future[List[Paths]] = driver.readSession { session =>
      c"""
        MATCH (a:Actor {tmdbId: $actorId})
        WITH a
        CALL {
          WITH a MATCH (a)-[KNOWS]-(m)
          RETURN m ORDER BY m.degree DESC LIMIT $f
        }
        CALL {
          WITH m MATCH (m)-[KNOWS]-(n)
          RETURN n ORDER BY n.degree DESC LIMIT $ff
        }
        WITH (collect(a.tmdbId) + collect(m.tmdbId) + collect(n.tmdbId)) AS actorIds
        MATCH (c)-[k:KNOWS]-(d)
        WHERE c.tmdbId IN actorIds AND d.tmdbId IN actorIds
        RETURN collect(c) AS nodes, k AS rels
      """
        .query[Paths]
        .list(session)
    }

    actorsPathsToGraph(friendsQuery)
  }

  def movies(tmdbIds: List[Long]): List[Movie] = {
    def moviesQuery = driver.readSession { session =>
      c"""
        MATCH r=(g:Genre)<--(m:Movie)<-[pi]-(:Actor) MATCH (c:Country)--(m)
        WHERE m.tmdbId IN $tmdbIds
        RETURN m, collect(distinct g), collect(distinct c), collect(distinct pi)
      """
        .query[
          (Movie, List[Genre], List[ProductionCountries], List[PlayInMovie])
        ]
        .list(session)
    }
    Await
      .result(moviesQuery, Duration.Inf)
      .map {
        case (m, gs, cs, pi) => {
          Movie(
            m.id,
            m.tmdbId,
            m.title,
            m.overview,
            m.budget,
            m.revenue,
            gs.sortWith(_.tmdbId < _.tmdbId),
            Some(Credits(pi.sortWith(_.order < _.order))),
            m.backdrop_path,
            m.poster_path,
            Some(cs.sortWith(_.iso_3166_1 < _.iso_3166_1)),
            m.release_date,
            m.runtime,
            m.tagline
          )
        }
      }
  }

  def search(
      criteria: String,
      limitActors: Int,
      limitMovies: Int
  ): List[ResultFormat] = {

    def searchActor: Future[List[Result]] = driver.readSession { session =>
      c"""
        MATCH (a:Actor)
        WHERE toLower(a.name) CONTAINS toLower($criteria)
        RETURN a.tmdbId as id, a.name as name, labels(a) as lbl
        LIMIT $limitActors
      """
        .query[Result]
        .list(session)
    }

    def searchMovie: Future[List[Result]] = driver.readSession { session =>
      c"""
        MATCH (m:Movie)
        WHERE toLower(m.title) CONTAINS toLower($criteria)
        RETURN m.tmdbId as id, m.title as name, labels(m) as lbl
        LIMIT $limitMovies
      """
        .query[Result]
        .list(session)
    }

    val actors = Await.result(searchActor, Duration.Inf)
    val movies = Await.result(searchMovie, Duration.Inf)
    val results = actors ::: movies

    results.map(r => ResultFormat(r.id, r.name, r.lbl.head))

  }
}
