package ch.master.hnv

import scala.concurrent.ExecutionContext.Implicits.global

import neotypes.Driver
import neotypes.GraphDatabase
import neotypes.implicits._
import org.neo4j.driver.AuthTokens

class Neo4j(host: String, user: String, password: String) {
  private val driver = GraphDatabase.driver(host, AuthTokens.basic(user, password))

  def hello = "hello"
}
