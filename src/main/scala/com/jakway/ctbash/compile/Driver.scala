package com.jakway.ctbash.compile

import java.io.File
import java.nio.file.Files

import com.jakway.ctbash.parser.{ParserError, ScalaExtractor}
import com.jakway.ctbash.util.Util

//TODO: implement Compiler
class Driver(val sourceFiles: Seq[File]) {
  val sources: Seq[String] = sourceFiles.map(scala.io.Source.fromFile(_).mkString)

  def run() = {
    import scalaz._
    import scalaz.std.AllInstances
    import scalaz.std.AllInstances._
    import scalaz.Monoid
    import scalaz.Monoid._
    import scalaz.syntax._
    for {
      //TODO: write collapseEithers with a generic error type
      srcs <- sources.map(ScalaExtractor.extractScala(_))

    } yield {
      ???
    }
  }
}
