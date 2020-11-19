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

class DataService(host: String, user: String, password: String) {
  private val driver =
    GraphDatabase.driver[Future](host, AuthTokens.basic(user, password))

  def hello = "hello"

  case class NodesPair(nodes: List[Node]) {
    val one = nodes.head
    val two = nodes.tail.head
  }
  case class Paths(nodes: NodesPair, rels: Relationship)

  def actors(limitMovie: Int, limitActor: Int, limitActorFriends: Int): Graph = {
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
        nm.get("id").asInstanceOf[Long],
        nm.get("name").asInstanceOf[String],
        Some(nm.get("biography").asInstanceOf[String]),
        Some(nm.get("birthday").asInstanceOf[String]),
        Some(nm.get("deathday").asInstanceOf[String]),
        nm.get("gender").asInstanceOf[String],
        Some(nm.get("place_of_birth").asInstanceOf[String]),
        Some(nm.get("profile_path").asInstanceOf[String]),
        None
      )
    }

    val paths = Await.result(actorsQuery, Duration.Inf)

    val neo4jNodes = mutable.Set[Node]()
    val neo4jRels = mutable.Set[Relationship]()

    paths.foreach(path => {
      val (node1, node2) = (path.nodes.one, path.nodes.two)
      neo4jNodes.add(node1)
      neo4jNodes.add(node2)

      val rel = path.rels
      neo4jRels.add(rel)
    })

    val nodes = neo4jNodes
      .map(node =>
        HnvNode(node.id, node.labels.asScala.toList, nodeToActor(node))
      )
      .toList

    val relationships = neo4jRels
      .map(rel =>
        KnowsRelation(
          rel.startNodeId,
          rel.endNodeId,
          rel.asMap.get("movieId").asInstanceOf[Long]
        )
      )
      .toList

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
