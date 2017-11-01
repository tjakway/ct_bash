package com.jakway.ctbash.compile

import java.nio.file.Files

import com.jakway.ctbash.parser.ScalaExtractor

//TODO: implement Compiler
class Driver(val source: String) {

  def run() = {
    for {
      (bashSrc, scalaSrc) <- ScalaExtractor.extractScala(source).right
    } yield {
      ???
    }
  }

  def apply() = run()

  //TODO: make outputDir a parameter
  val tempDirPrefix = "ctbash"
  lazy val outputDir = {
    val f = Files.createTempDirectory(tempDirPrefix)
    f.toFile.deleteOnExit()
    f
  }

}
