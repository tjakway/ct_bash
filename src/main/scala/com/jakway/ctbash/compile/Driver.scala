package com.jakway.ctbash.compile

import java.io.File
import java.nio.file.Files

import com.jakway.ctbash.GeneralOptions
import com.jakway.ctbash.parser.{ParserError, ScalaExtractor}
import com.jakway.ctbash.util.Util

//TODO: implement Compiler
class Driver(val opts: GeneralOptions, val sourceFiles: Vector[File]) {
  val sources: Vector[String] = sourceFiles.map(scala.io.Source.fromFile(_).mkString)

  def run() = {
    for {
      srcs <- ScalaExtractor.extractScala(sources)


    } yield {
      ???
    }
  }
}
