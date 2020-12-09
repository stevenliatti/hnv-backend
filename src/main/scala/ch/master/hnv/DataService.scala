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
import neotypes.implicits.mappers.all._
import neotypes.implicits.syntax.string._
import neotypes.implicits.syntax.cypher._
import neotypes.GraphDatabase
import org.neo4j.driver.AuthTokens
import org.neo4j.driver.types.Node
import org.neo4j.driver.types.Relationship
import spray.json.JsonParser
import neotypes.DeferredQuery
import neotypes.DeferredQueryBuilder

class DataService(host: String) {
  private val driver = GraphDatabase.driver[Future](host)

  def hello = "hello"

  private def nodeToActor(node: Node): Actor = {
    val nm = node.asMap
    Actor(
      node.id,
      nm.get("tmdbId").asInstanceOf[Long],
      nm.get("name").asInstanceOf[String],
      Some(nm.getOrDefault("biography", "").asInstanceOf[String]),
      Some(nm.getOrDefault("birthday", "").asInstanceOf[String]),
      Some(nm.getOrDefault("deathday", "").asInstanceOf[String]),
      nm.get("gender").asInstanceOf[String],
      Some(nm.getOrDefault("place_of_birth", "").asInstanceOf[String]),
      Some(nm.getOrDefault("profile_path", "").asInstanceOf[String]),
      None,
      nm.get("knowsDegree").asInstanceOf[Long],
      nm.get("playInDegree").asInstanceOf[Long],
      nm.get("degree").asInstanceOf[Long],
      // TODO try with other communities if needed
      nm.get("knowsCommunityModularityOptimization").asInstanceOf[Long]
    )
  }

  private def nodeToMovie(node: Node): Movie = {
    val nm = node.asMap
    Movie(
      node.id,
      nm.get("tmdbId").asInstanceOf[Long],
      nm.get("title").asInstanceOf[String],
      nm.get("overview").asInstanceOf[String],
      nm.get("budget").asInstanceOf[Long],
      nm.get("revenue").asInstanceOf[Long],
      Nil,
      None,
      Some(nm.getOrDefault("backdrop_path", "").asInstanceOf[String]),
      Some(nm.getOrDefault("poster_path", "").asInstanceOf[String]),
      None,
      Some(nm.getOrDefault("release_date", "").asInstanceOf[String]),
      Some(nm.getOrDefault("runtime", "").asInstanceOf[Long]),
      Some(nm.getOrDefault("tagline", "").asInstanceOf[String])
    )
  }

  private def relationshipToPlayIn(rel: Relationship): PlayInMovie = {
    val rm = rel.asMap
    val character = rm.getOrDefault("character", "").asInstanceOf[String]
    PlayInMovie(
      rel.startNodeId,
      if (character.isEmpty) None else Some(character),
      rm.get("order").asInstanceOf[Long]
    )
  }

  private def relationshipsToKnowsRelation(
      relationships: List[Relationship]
  ): List[KnowsRelation] = {
    val relsMap = mutable.Map[PairIds, List[Long]]()

    relationships.foreach(r => {
      val pairIds = PairIds(r.startNodeId, r.endNodeId)
      val movieId = r.asMap.get("movieId").asInstanceOf[Long]

      if (relsMap.contains(pairIds)) {
        relsMap.put(
          pairIds,
          movieId :: relsMap(pairIds)
        )
      } else {
        relsMap.put(pairIds, List(movieId))
      }
    })

    relsMap.map { case (pairIds, list) =>
      KnowsRelation(pairIds.one, pairIds.another, list)
    }.toList
  }

  private def actorsPathsToGraph(fPaths: Future[List[Paths]]) = {
    val paths = Await.result(fPaths, Duration.Inf)

    val nodes = paths
      .flatMap(_.nodes)
      .toSet
      .map((node: Node) => HnvNode(nodeToActor(node)))
      .toList

    val relationships =
      relationshipsToKnowsRelation(paths.map(_.rels)).map(RelData(_))

    Graph(nodes, relationships)
  }

  private def filterMovieDates(
      movieLabel: String,
      movieStartDate: Option[String],
      movieEndDate: Option[String]
  ): Option[String] = (movieStartDate, movieEndDate) match {
    case (Some(start), Some(end)) =>
      Some(
        s"date($movieLabel.release_date) >= date('$start') AND date($movieLabel.release_date) <= date('$end')"
      )
    case (Some(start), None) =>
      Some(s"date($movieLabel.release_date) >= date('$start')")
    case (None, Some(end)) =>
      Some(s"date($movieLabel.release_date) <= date('$end')")
    case _ => None
  }

  private def filterMovieBudget(
      movieLabel: String,
      minBudget: Option[Int],
      maxBudget: Option[Int]
  ): Option[String] = (minBudget, maxBudget) match {
    case (Some(min), Some(max)) =>
      Some(s"$movieLabel.budget >= $min AND $movieLabel.budget <= $max")
    case (Some(min), None) => Some(s"$movieLabel.budget >= $min")
    case (None, Some(max)) => Some(s"$movieLabel.budget <= $max)")
    case _                 => None
  }

  private def filterMovieRevenue(
      movieLabel: String,
      minRevenue: Option[Int],
      maxRevenue: Option[Int]
  ): Option[String] = (minRevenue, maxRevenue) match {
    case (Some(min), Some(max)) =>
      Some(s"$movieLabel.revenue >= $min AND $movieLabel.revenue <= $max")
    case (Some(min), None) => Some(s"$movieLabel.revenue >= $min")
    case (None, Some(max)) => Some(s"$movieLabel.revenue <= $max)")
    case _                 => None
  }

  private def filterMovieRuntime(
      movieLabel: String,
      minRuntime: Option[Int],
      maxRuntime: Option[Int]
  ): Option[String] = (minRuntime, maxRuntime) match {
    case (Some(min), Some(max)) =>
      Some(s"$movieLabel.runtime >= $min AND $movieLabel.runtime <= $max")
    case (Some(min), None) => Some(s"$movieLabel.runtime >= $min")
    case (None, Some(max)) => Some(s"$movieLabel.runtime <= $max)")
    case _                 => None
  }

  private def filterMovieGenres(
      genreLabel: String,
      genres: Option[String]
  ): Option[String] = genres match {
    case Some(list) =>
      Some(
        s"$genreLabel.name IN [${list.split(",").toList.map(g => s"'$g'").mkString(",")}]"
      )
    case None => None
  }

  private def filterActorGender(
      actorLabel: String,
      gender: Option[String]
  ): Option[String] = gender match {
    case Some(value) => Some(s"$actorLabel.gender = '$value'")
    case None        => None
  }

  private def filterActorBirth(
      actorLabel: String,
      startBirth: Option[String],
      endBirth: Option[String]
  ): Option[String] = (startBirth, endBirth) match {
    case (Some(start), Some(end)) =>
      Some(
        s"date($actorLabel.birthday) >= date('$start') AND date($actorLabel.birthday) <= date('$end')"
      )
    case (Some(start), None) =>
      Some(s"date($actorLabel.birthday) >= date('$start')")
    case (None, Some(end)) =>
      Some(s"date($actorLabel.birthday) <= date('$end')")
    case _ => None
  }

  private def filterActorDeath(
      actorLabel: String,
      startDeath: Option[String],
      endDeath: Option[String]
  ): Option[String] = (startDeath, endDeath) match {
    case (Some(start), Some(end)) =>
      Some(
        s"date($actorLabel.deathday) >= date('$start') AND date($actorLabel.deathday) <= date('$end')"
      )
    case (Some(start), None) =>
      Some(s"date($actorLabel.deathday) >= date('$start')")
    case (None, Some(end)) =>
      Some(s"date($actorLabel.deathday) <= date('$end')")
    case _ => None
  }

  private def filterActorCountryOrigin(
      actorLabel: String,
      origin: Option[String]
  ): Option[String] = origin match {
    case Some(value) => Some(s"$actorLabel.place_of_birth CONTAINS '$value'")
    case None        => None
  }

  private def concatFilters(
      someQueries: List[Option[String]]
  ): Option[String] = {
    val qs = someQueries.filter(_.isDefined).map(o => o.get)
    qs match {
      case Nil => None
      case head :: tail =>
        Some(
          tail.foldLeft(s"WHERE ($head)")((init, q) =>
            init + s" AND (" + q + s")"
          )
        )
    }
  }

  def actorsGraph(
      limitMovie: Option[Int] = Some(5),
      limitActor: Option[Int] = Some(3),
      limitActorFriends: Option[Int] = Some(1),
      movieStartDate: Option[String] = None,
      movieEndDate: Option[String] = None,
      movieMinBudget: Option[Int] = None,
      movieMaxBudget: Option[Int] = None,
      movieMinRevenue: Option[Int] = None,
      movieMaxRevenue: Option[Int] = None,
      movieMinRuntime: Option[Int] = None,
      movieMaxRuntime: Option[Int] = None,
      movieGenres: Option[String] = None,
      actorGender: Option[String] = None,
      actorStartBirth: Option[String] = None,
      actorEndBirth: Option[String] = None,
      actorStartDeath: Option[String] = None,
      actorEndDeath: Option[String] = None,
      actorCountryOrigin: Option[String] = None
  ): Graph = {

    val lm = if (limitMovie.getOrElse(5) > 20) 20 else limitMovie.getOrElse(5)
    val la = if (limitActor.getOrElse(3) > 20) 20 else limitActor.getOrElse(3)
    val laf =
      if (limitActorFriends.getOrElse(1) > 10) 10
      else limitActorFriends.getOrElse(1)

    val movieFilters = concatFilters(
      List(
        filterMovieDates("m", movieStartDate, movieEndDate),
        filterMovieBudget("m", movieMinBudget, movieMaxBudget),
        filterMovieRevenue("m", movieMinRevenue, movieMaxRevenue),
        filterMovieRuntime("m", movieMinRuntime, movieMaxRuntime),
        filterMovieGenres("g", movieGenres)
      )
    )

    val actorFilters = concatFilters(
      List(
        filterActorGender("a", actorGender),
        filterActorBirth("a", actorStartBirth, actorEndBirth),
        filterActorDeath("a", actorStartDeath, actorEndDeath),
        filterActorCountryOrigin("a", actorCountryOrigin)
      )
    )

    val actorFilters2 = concatFilters(
      List(
        filterActorGender("b", actorGender),
        filterActorBirth("b", actorStartBirth, actorEndBirth),
        filterActorDeath("b", actorStartDeath, actorEndDeath),
        filterActorCountryOrigin("b", actorCountryOrigin)
      )
    )

    def actorsQuery: Future[List[Paths]] = driver.readSession { session =>
      val rest = movieFilters.getOrElse("") +
        s"""
        WITH m ORDER BY m.revenue DESC LIMIT $lm
        CALL {
          WITH m MATCH (m)<-[p]-(a:Actor)
        """ + actorFilters.getOrElse("") +
        s"""
          RETURN a ORDER BY p.order LIMIT $la
        }
        CALL {
          WITH a MATCH (a)-[k:KNOWS]-(b)
        """ + actorFilters2.getOrElse("") +
        s"""
          RETURN k,b ORDER BY b.degree DESC LIMIT $laf
        }
        WITH (collect(a.tmdbId) + collect(b.tmdbId)) AS actorIds
        MATCH (c:Actor)-[k2:KNOWS]-(d)
        WHERE c.tmdbId IN actorIds AND d.tmdbId IN actorIds
        RETURN collect(c) AS nodes, k2 AS rels
      """
      // println("MATCH (m:Movie)--(g:Genre) " + rest)
      (c"MATCH (m:Movie)--(g:Genre) " + rest).query[Paths].list(session)
    }

    actorsPathsToGraph(actorsQuery)
  }

  def movieGraph(tmdbId: Long): Graph = {
    def movieQuery: Future[(List[Node], List[Relationship])] =
      driver.readSession { session =>
        c"""
          MATCH
            (g:Genre)<-[bt]-(m:Movie)<-[pli]-(a:Actor),
            (c:Country)-[pri]-(m)
          WHERE m.tmdbId = $tmdbId
          RETURN (
            collect(DISTINCT m) +
            collect(DISTINCT a) +
            collect(DISTINCT g) +
            collect(DISTINCT c)
          ) AS nodes,
          (
            collect(DISTINCT bt) +
            collect(DISTINCT pli) +
            collect(DISTINCT pri)
          ) AS rels
        """
          .query[(List[Node], List[Relationship])]
          .single(session)
      }

    val (nodes, relationships) = Await.result(movieQuery, Duration.Inf)

    val hnvNodes = nodes
      .map(node =>
        node.labels.asScala.toList.head match {
          case "Movie" => nodeToMovie(node)
          case "Actor" => nodeToActor(node)
          case "Genre" =>
            Genre(
              node.id,
              node.asMap.get("tmdbId").asInstanceOf[Long],
              node.asMap.get("name").asInstanceOf[String],
              node.asMap.get("belongsToDegree").asInstanceOf[Long],
              node.asMap.get("knownForDegree").asInstanceOf[Long],
              node.asMap.get("degree").asInstanceOf[Long]
            )
          case "Country" =>
            ProductionCountry(
              node.id,
              node.asMap.get("iso_3166_1").asInstanceOf[String],
              node.asMap.get("name").asInstanceOf[String]
            )
        }
      )
      .map(HnvNode(_))

    val edges = relationships
      .map(rel =>
        rel.`type` match {
          case "BELONGS_TO" => BelongsToRelation(rel.startNodeId, rel.endNodeId)
          case "PLAY_IN" =>
            PlayInRelation(
              rel.startNodeId,
              rel.endNodeId,
              Some(
                rel.asMap.getOrDefault("character", "").asInstanceOf[String]
              ),
              rel.asMap.get("order").asInstanceOf[Long]
            )
          case "PRODUCED_IN" =>
            ProducedInRelation(rel.startNodeId, rel.endNodeId)
        }
      )
      .map(RelData(_))

    Graph(hnvNodes, edges)
  }

  def friendsOfGraph(
      actorId: Long,
      friends: Int,
      friendsOfFriends: Int
  ): Graph = {

    val f = if (friends > 30) 30 else friends
    val ff = if (friendsOfFriends > 15) 15 else friendsOfFriends

    def friendsQuery: Future[List[Paths]] = driver.readSession { session =>
      c"""
        MATCH (a:Actor {tmdbId: $actorId})
        WITH a
        CALL {
          WITH a MATCH (a)-[c:KNOWS_COUNT]-(f)
          RETURN f ORDER BY c.count DESC LIMIT $f
        }
        CALL {
          WITH f MATCH (f)-[c:KNOWS_COUNT]-(ff)
          RETURN ff ORDER BY c.count DESC LIMIT $ff
        }
        WITH (collect(a.tmdbId) + collect(f.tmdbId) + collect(ff.tmdbId)) AS actorIds
        MATCH (x)-[k:KNOWS]-(y)
        WHERE x.tmdbId IN actorIds AND y.tmdbId IN actorIds
        RETURN DISTINCT (collect(x) + collect(y)) AS nodes, k AS rels
      """
        .query[Paths]
        .list(session)
    }

    actorsPathsToGraph(friendsQuery)
  }

  def movies(tmdbIds: List[Long]): List[MovieWithActors] = {
    def moviesQuery = driver.readSession { session =>
      c"""
        MATCH r=(g:Genre)<--(m:Movie)<-[pi]-(a:Actor) MATCH (c:Country)--(m)
        WHERE m.tmdbId IN $tmdbIds
        RETURN m, collect(distinct a), collect(distinct g), collect(distinct c), collect(distinct pi)
      """
        .query[
          (
              Movie,
              List[Node],
              List[Genre],
              List[ProductionCountry],
              List[Relationship]
          )
        ]
        .list(session)
    }
    Await
      .result(moviesQuery, Duration.Inf)
      .map {
        case (m, ns, gs, cs, pis) => {
          MovieWithActors(
            Movie(
              m.id,
              m.tmdbId,
              m.title,
              m.overview,
              m.budget,
              m.revenue,
              gs.sortWith(_.tmdbId < _.tmdbId),
              Some(
                Credits(
                  pis
                    .map(pi => relationshipToPlayIn(pi))
                    .sortWith(_.order < _.order)
                )
              ),
              m.backdrop_path,
              m.poster_path,
              Some(cs.sortWith(_.iso_3166_1 < _.iso_3166_1)),
              m.release_date,
              m.runtime,
              m.tagline
            ),
            ns.map(n => nodeToActor(n)).sortWith(_.degree < _.degree)
          )
        }
      }
  }

  def actors(tmdbIds: List[Long]): List[ActorWithRelative] = {
    def actorsQuery = driver.readSession { session =>
      c"""
        MATCH r=(m:Movie)<-[pi]-(a:Actor)-[k:KNOWS]-(friends:Actor) MATCH (g:Genre)-[kf]-(a)
        WHERE a.tmdbId IN $tmdbIds
        RETURN
          a,
          collect(distinct friends),
          collect(distinct m),
          collect(distinct g),
          collect(distinct pi),
          collect(distinct k),
          collect(distinct kf)
      """
        .query[
          (
              Node,
              List[Node],
              List[Movie],
              List[Genre],
              List[Relationship],
              List[Relationship],
              List[Relationship]
          )
        ]
        .list(session)
    }
    Await
      .result(actorsQuery, Duration.Inf)
      .map {
        case (a, friends, m, g, pi, k, kf) => {
          val knowsRelations = relationshipsToKnowsRelation(k)

          ActorWithRelative(
            nodeToActor(a),
            friends.map(f => nodeToActor(f)),
            m,
            g,
            pi.map(p =>
              PlayInRelation(
                p.startNodeId,
                p.endNodeId,
                if (p.asMap.containsKey("character"))
                  Some(p.asMap.get("character").asInstanceOf[String])
                else None,
                p.asMap.get("order").asInstanceOf[Long]
              )
            ),
            knowsRelations,
            kf.map(p =>
              KnownForRelation(
                p.startNodeId,
                p.endNodeId,
                p.asMap.get("count").asInstanceOf[Long]
              )
            )
          )
        }
      }
  }

  def search(
      criteria: String,
      limitActors: Int,
      limitMovies: Int
  ): List[ResultFormat] = {

    def searchActor: Future[List[Result]] = driver.readSession { session =>
      c"""
        MATCH (a:Actor)
        WHERE toLower(a.name) CONTAINS toLower($criteria)
        RETURN a.tmdbId as id, a.name as name, labels(a) as lbl
        LIMIT $limitActors
      """
        .query[Result]
        .list(session)
    }

    def searchMovie: Future[List[Result]] = driver.readSession { session =>
      c"""
        MATCH (m:Movie)
        WHERE toLower(m.title) CONTAINS toLower($criteria)
        RETURN m.tmdbId as id, m.title as name, labels(m) as lbl
        LIMIT $limitMovies
      """
        .query[Result]
        .list(session)
    }

    val actors = Await.result(searchActor, Duration.Inf)
    val movies = Await.result(searchMovie, Duration.Inf)
    val results = actors ::: movies

    results.map(r => ResultFormat(r.id, r.name, r.lbl.head))

  }

  def shortestPath(actorId1: Long, actorId2: Long): Graph = {

    def shortestPathQuery: Future[(List[Node], List[Relationship])] =
      driver.readSession { session =>
        c"""
          MATCH
          (a1:Actor {tmdbId: $actorId1}),
          (a2:Actor {tmdbId: $actorId2}),
          p = shortestPath((a1)-[:KNOWS*]-(a2))
          WITH nodes(p) AS n
          MATCH (a:Actor)-[k:KNOWS]-(b:Actor) WHERE a IN n AND b IN n
          RETURN collect(DISTINCT a) AS nodes, collect(DISTINCT k) AS relationships
        """.query[(List[Node], List[Relationship])].single(session)
      }

    val (nodes, relationships) = Await.result(shortestPathQuery, Duration.Inf)
    val hnvNodes = nodes.map(n => HnvNode(nodeToActor(n)))
    val relations = relationshipsToKnowsRelation(relationships).map(RelData(_))

    Graph(hnvNodes, relations)
  }

  def placesOfBirth: List[String] = {
    def placesOfBirthQuery: Future[List[String]] =
      driver.readSession { session =>
        c"""
          MATCH (a:Actor) RETURN DISTINCT a.place_of_birth
        """.query[String].list(session)
      }

    Await
      .result(placesOfBirthQuery, Duration.Inf)
      .map(place =>
        place
          .split(",")
          .toList
          .last
          .split("-")
          .toList
          .last
          .split(",")
          .toList
          .last
          .trim
          .replaceAll("[\\[\\](),-.]", "")
      )
      .distinct
      .sorted
  }
}
