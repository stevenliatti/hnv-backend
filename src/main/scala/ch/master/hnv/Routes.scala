package ch.master.hnv

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import ch.megard.akka.http.cors.scaladsl.model.HttpOriginMatcher
import ch.megard.akka.http.cors.scaladsl.settings.CorsSettings
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.unmarshalling.PredefinedFromStringUnmarshallers.CsvSeq

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
      path("apidoc.yaml") {
        getFromFile("apidoc.yaml")
      },
      path("actors") {
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
      },
      path("friendsOf" / LongNumber) { tmdbId: Long =>
        get {
          parameters(
            "friends".as[Int].?,
            "friendsOfFriends".as[Int].?
          ) { (friends, friendsOfFriends) =>
            complete(
              (
                StatusCodes.OK,
                (friends, friendsOfFriends) match {
                  case (Some(f), Some(ff)) =>
                    dataService.friendsOf(tmdbId, f, ff)
                  case _ => dataService.friendsOf(tmdbId, 20, 10)
                }
              )
            )
          }
        }
      },
      path("movie" / LongNumber) { tmdbId: Long =>
        get {
          dataService.movies(List(tmdbId)) match {
            case h :: _ => complete((StatusCodes.OK, h))
            case Nil =>
              complete((StatusCodes.NotFound, Empty("movie not found")))
          }
        }
      },
      path("movies") {
        get {
          parameters("tmdbIds".as(CsvSeq[String])) { tmdbIds =>
            val longIds = tmdbIds.map(i => i.toLong).toList
            complete((StatusCodes.OK, dataService.movies(longIds)))
          }
        }
      },
      path("search") {
        get {
          parameters("criteria".as[String], "limitActors".as[Int], "limitMovies".as[Int]) { (criteria, limitActors, limitMovies) =>
            val res = dataService.search(criteria, limitActors, limitMovies)
            complete((StatusCodes.OK, res))
          }
        }
      }
    )
  }
}
