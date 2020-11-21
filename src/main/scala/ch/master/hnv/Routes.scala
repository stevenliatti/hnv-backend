package ch.master.hnv

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import ch.megard.akka.http.cors.scaladsl.model.HttpOriginMatcher
import ch.megard.akka.http.cors.scaladsl.settings.CorsSettings


class Routes(val dataService: DataService)(implicit
    val system: ActorSystem[_]
) {

  import JsonFormats._
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

  // If ask takes more time than this to complete the request is failed
  private implicit val timeout = Timeout.create(
    system.settings.config.getDuration("my-app.routes.ask-timeout")
  )

  lazy val corsSettings: CorsSettings = CorsSettings.defaultSettings
    .withAllowedOrigins(HttpOriginMatcher.*)
    .withAllowedMethods(
      scala.collection.immutable.Seq(OPTIONS, POST, PUT, GET, DELETE)
    )

  val routes: Route = cors(corsSettings) {
    concat(
      path("hello") {
        get {
          complete((StatusCodes.OK, dataService.hello))
        }
      },
      path("") {
        get {
          parameters(
            "limitMovie".as[Int].?,
            "limitActor".as[Int].?,
            "limitActorFriends".as[Int].?
          ) { (limitMovie, limitActor, limitActorFriends) =>
            complete(
              (
                StatusCodes.OK,
                (limitMovie, limitActor, limitActorFriends) match {
                  case (Some(lm), Some(la), Some(laf)) =>
                    dataService.actors(lm, la, laf)
                  case _ => dataService.actors(5, 3, 1)
                }
              )
            )
          }
        }
      }
    )
  }
}
