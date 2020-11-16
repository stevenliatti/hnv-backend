package ch.master.hnv

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout

class Routes(val neo4j: Neo4j)(implicit val system: ActorSystem[_]) {

  import JsonFormats._
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

  // If ask takes more time than this to complete the request is failed
  private implicit val timeout = Timeout.create(
    system.settings.config.getDuration("my-app.routes.ask-timeout")
  )

  val routes: Route =
    // pathPrefix("api") {
      concat(
        path("hey") {
          post {
            entity(as[String]) { _ =>
              complete("Hey")
            }
          }
        },
        path("hello") {
          get {
            complete(neo4j.hello)
          }
        }
      )
    // }
}
