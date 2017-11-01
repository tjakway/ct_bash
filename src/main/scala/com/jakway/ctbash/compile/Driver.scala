package com.jakway.ctbash.compile

import java.io.File
import java.nio.file.Files

import com.jakway.ctbash.parser.ScalaExtractor

//TODO: implement Compiler
class Driver(val sourceFiles: Seq[File]) {
  val sources: Seq[String] = sourceFiles.map(scala.io.Source.fromFile(_).mkString)

  def run() = {
    for {
      //TODO: write collapseEithers with a generic error type
      (bashSrc, scalaSrc) <- sources.map(ScalaExtractor.extractScala(_))

    } yield {
      ???
    }
  }
}
