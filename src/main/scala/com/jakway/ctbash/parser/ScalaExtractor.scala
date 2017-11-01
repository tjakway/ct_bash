package com.jakway.ctbash.parser

import com.jakway.ctbash.compile.{BashSource, CompileError, ScalaSource}
import com.jakway.ctbash.parser.ParserError.ScalaTagWithoutBraces
import com.jakway.ctbash.util.Util

import scala.util.matching.Regex
import scalaz.{Equal, Monoid}

sealed trait ParserError extends CompileError {
  val where: (StringPosition, StringPosition)
}

object ParserError {
  case class ScalaTagWithoutBraces(override val where: (StringPosition, StringPosition)) extends ParserError {
    override val description: String = s"Scala tag found without closing braces at ${StringPosition.fmt(where)} (did you forget to add them?)"
  }

  case class OnlyScalaTag(override val where: (StringPosition, StringPosition))
    extends ParserError {
    override val description: String = s"Only an empty @scala tag was found"
  }
}

class ErrorChecks(val source: String) {
  import ParserError._

  type CheckRegex = String => Seq[ParserError]

  val ranges = StringPosition.lineNumberRanges(source)

  def mkCheckRegex(r: Regex, f: ((StringPosition, StringPosition)) => ParserError):
    CheckRegex = { s =>
    val matches = r.findAllMatchIn(s)
    if(matches.isEmpty)
      Seq()
    else {
      //return all errors found by this error checker
      StringPosition.matchesToStringPosition(source, matches, Some(ranges))
        .map(f(_))
        .toSeq
    }
  }

  val scalaTagWithoutBraces: CheckRegex =
    mkCheckRegex("""(?siU)@scala(?<=\s*[\p{Alnum}\p{Punct}]+)""".r,
      ScalaTagWithoutBraces)

  val onlyScalaTag: CheckRegex =
    mkCheckRegex("""(?siU)@scala\s*""".r,
      OnlyScalaTag)

  def checkAll(): Seq[ParserError] = {
    val empty: Seq[ParserError] = Seq()

    //TODO: add checks to this list
    Seq(scalaTagWithoutBraces, onlyScalaTag)
      .foldLeft(empty) {
      case (acc, thisCheck) => acc ++ thisCheck(source)
    }
  }
}

object ScalaExtractor {
  val rgx = """(.*)@scala\s*{(.*)}(.*)""".r

  def hasScalaTag(src: String): Boolean =
    src.contains("@scala")


  def extractScala(srcs: Vector[String]): Either[Vector[ParserError],
    Vector[(Option[BashSource], Option[ScalaSource])]] = {

    import scalaz._
    import scalaz.std.AllInstances._

    //WTF scalaz?
    implicit val e = new Equal[Vector[ParserError]] {
      override def equal(a1: Vector[ParserError], a2: Vector[ParserError]) = a1 == a2
    }

    Util.accumulateEithers(srcs.map(s => extractScala(s) match {
      case Left(e) => Left(e.toVector)
      case Right(z) => Right(Vector(z))
    }))
  }

  /**
    *
    * @param src
    * @return Option[src] in case there isn't any of that source, in which case we can skip compilation
    */
  def extractScala(src: String):
    Either[Seq[ParserError], (Option[BashSource], Option[ScalaSource])] = {

    //check for any parser errors before doing further work
    val errors = new ErrorChecks(src).checkAll()
    if(!errors.isEmpty) {
      Left(errors)
    }
      //proceed with parsing
    else {
      def helper(accBash: Seq[String], accScala: Seq[String])(p: String): (Seq[String], Seq[String]) = {
        rgx.findFirstMatchIn(p) match {
          case Some(m) => {
            //regex groups apparently count from 1, see https://stackoverflow.com/questions/3050907/scala-capture-group-using-regex
            val (before, scala, after) = (Option(m.group(1)), Option(m.group(2)), Option(m.group(3)))

            val newAccBash = accBash :+ before.getOrElse("")
            val newAccScala = accScala :+ scala.getOrElse("")

            val rest = after.getOrElse("")
            if (rest.isEmpty) {
              (newAccBash, newAccScala)
            }
            else {
              helper(newAccBash, newAccScala)(rest)
            }
          }
          case None => (accBash, accScala)
        }
      }

      val (bash, scala) = helper(Seq(), Seq())(src)

      def filterConcat[A](x: Seq[String])(f: String => A): Option[A] =
        Option(x)
          //return None if the Seq is empty
          .filter(!_.isEmpty)
          //join the sources with newlines
          .map(_.fold("")(_ + "\n" + _))
          //apply the constructor
          .map(f)


      Right((filterConcat(bash)(BashSource), filterConcat(scala)(ScalaSource)))
    }
  }
}
