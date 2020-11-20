package ch.master.hnv

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route

import scala.util.Failure
import scala.util.Success

object Main {
  private def startHttpServer(
      interface: String,
      port: Int,
      routes: Route,
      system: ActorSystem[_]
  ): Unit = {

    implicit val classicSystem: akka.actor.ActorSystem = system.toClassic
    import system.executionContext

    val futureBinding = Http().bindAndHandle(routes, interface, port)
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
    val (envNeo4jHost, envInterface, envPort) = (
      sys.env.get("NEO4J_HOST"),
      sys.env.get("BACKEND_INTERFACE"),
      sys.env.get("BACKEND_PORT")
    )
    (envNeo4jHost, envInterface, envPort) match {
      case (Some(neo4jHost), Some(interface), Some(strPort)) =>
        println(s"$neo4jHost, $interface, $strPort")
        val dataService = new DataService(neo4jHost)
        val port = Integer.parseInt(strPort)

        val rootBehavior = Behaviors.setup[Nothing] { context =>
          val routes = new Routes(dataService)(context.system)
          startHttpServer(interface, port, routes.routes, context.system)
          Behaviors.empty
        }
        val system = ActorSystem[Nothing](rootBehavior, "HNVBackend")

      case _ =>
        println("You have to define host env variable")
        sys.exit(42)
    }
  }
}
