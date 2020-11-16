package ch.master.hnv

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route

import scala.util.Failure
import scala.util.Success

object Main {
  private def startHttpServer(routes: Route, system: ActorSystem[_]): Unit = {

    implicit val classicSystem: akka.actor.ActorSystem = system.toClassic
    import system.executionContext

    // val futureBinding = Http().bindAndHandle(routes, "0.0.0.0", 8080)
    val futureBinding = Http().bindAndHandle(routes, "localhost", 8080)
    futureBinding.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info(
          "Server online at http://{}:{}/",
          address.getHostString,
          address.getPort
        )
      case Failure(ex) =>
        system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
        system.terminate()
    }
  }

  def main(args: Array[String]): Unit = {
    val (neo4jHost, neo4jUser, neo4jPassword) = (
      sys.env.get("NEO4J_HOST"),
      sys.env.get("NEO4J_USER"),
      sys.env.get("NEO4J_PASSWORD")
    )
    val dataService = (neo4jHost, neo4jUser, neo4jPassword) match {
      case (Some(host), Some(user), Some(password)) =>
        new DataService(host, user, password)
      case _ =>
        println("You have to define env variables")
        sys.exit(42)
    }

    val rootBehavior = Behaviors.setup[Nothing] { context =>
      // val userRegistryActor = context.spawn(UserRegistry(), "UserRegistryActor")
      //  context.watch(userRegistryActor)

      val routes = new Routes(dataService)(context.system)
      startHttpServer(routes.routes, context.system)

      Behaviors.empty
    }
    val system = ActorSystem[Nothing](rootBehavior, "HelloAkkaHttpServer")
  }
}
