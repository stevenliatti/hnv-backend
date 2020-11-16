package ch.master.hnv
import scala.collection.mutable

object Domain {
  trait Properties

  case class PlayInMovie(id: Long, character: Option[String], order: Int)
  case class Credits(cast: List[PlayInMovie])

  case class Actor(
      id: Long,
      name: String,
      biography: Option[String],
      birthday: Option[String],
      deathday: Option[String],
      gender: String,
      place_of_birth: Option[String],
      profile_path: Option[String],
      movie_credits: Option[Credits]
  ) extends Properties

  case class Movie(
      id: Long,
      title: String,
      overview: String,
      budget: Long,
      revenue: Long,
      genres: List[Genre],
      credits: Option[Credits],
      backdrop_path: Option[String],
      poster_path: Option[String],
      release_date: Option[String],
      runtime: Option[Int],
      tagline: Option[String]
  ) extends Properties

  case class Genre(id: Long, name: String) extends Properties

  case class Graph(nodes: Seq[Node], relationships: Seq[Relation])
  case class Relation(movieId: String, source: String, target: String)
  case class Node(id: String, label: String, properties: Properties)

}
