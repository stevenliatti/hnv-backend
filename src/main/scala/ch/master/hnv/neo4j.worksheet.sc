import org.neo4j.driver.types.Node
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

// import ch.master.hnv.Domain._

val driver = GraphDatabase
  .driver[Future]("bolt://192.168.1.129:7687", AuthTokens.basic("neo4j", "wem2020"))

case class Graph(nodes: List[org.neo4j.driver.types.Node], rels: org.neo4j.driver.types.Relationship, rels2: org.neo4j.driver.types.Relationship)

def test = driver.readSession { session =>
  c"""
    MATCH (m:Movie)
    WITH m ORDER BY m.revenue DESC LIMIT 3
    CALL {
      WITH m MATCH (m)<--(a:Actor)
      RETURN a LIMIT 2
    }
    CALL {
      WITH a MATCH (a)-[k:KNOWS]-(b)
      RETURN k,b LIMIT 1
    }
    WITH (collect(a) + collect(b)) AS nodes, k AS rels
    UNWIND nodes as nn
    MATCH (nn)-[k2]-()
    RETURN nn, rels, k2 AS rels2
  """
  .query[Graph]
  //.query[Actor]
  .list(session)
}
val nodes = Await.result(test, Duration.Inf)
println(nodes.length)
nodes.foreach(asdf => {
  // println("neoId: " + asdf.nodes.id + ", id: " + asdf.nodes.asMap.get("id") + ", " + asdf.nodes.asMap.get("name"))
  asdf.nodes.foreach(node => {
    println(s"${node.asMap.get("name")} (${node.id})")
  })
  println(asdf.rels.startNodeId() + "," + asdf.rels.endNodeId() + ", " + asdf.rels.asMap)
  println("\n\n")
})




// def nodeToActor(node: org.neo4j.driver.types.Node) = {
//   val nm = node.asMap
//   val id = nm.get("id")
//   println(id)
//   Actor(
//     nm.get("id").asInstanceOf[Long],
//     nm.get("name").asInstanceOf[String],
//     Some(nm.get("birthday").asInstanceOf[String]),
//     Some(nm.get("biography").asInstanceOf[String]),
//     Some(nm.get("deathday").asInstanceOf[String]),
//     nm.get("gender").asInstanceOf[String],
//     Some(nm.get("place_of_birth").asInstanceOf[String]),
//     Some(nm.get("profile_path").asInstanceOf[String]),
//     None,
//   )
// }

//val actors = nodes.map(n => nodeToActor(n))
//val actors = nodes.foreach(n=> println(n.name))
