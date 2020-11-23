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
import neotypes.GraphDatabase
import neotypes.implicits.all._
import org.neo4j.driver.AuthTokens
import org.neo4j.driver.types.Node
import org.neo4j.driver.types.Relationship
import spray.json.JsonParser

class DataService(host: String) {
  private val driver = GraphDatabase.driver[Future](host)

  def hello = "hello"

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

    def nodeToActor(node: Node): Actor = {
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
        nm.get("knowsCommunity").asInstanceOf[Long]
      )
    }

    val paths = Await.result(actorsQuery, Duration.Inf)

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
      .map(node =>
        HnvNode(nodeToActor(node))
      )
      .toList

    val relationships = neo4jRels.map { case (pairIds, list) =>
      RelData(KnowsRelation(pairIds.one, pairIds.another, list))
    }.toList

    Graph(nodes, relationships)
  }

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
  ): Graph = ???

}
