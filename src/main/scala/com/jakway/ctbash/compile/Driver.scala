package com.jakway.ctbash.compile

import java.nio.file.Files

import com.jakway.ctbash.parser.ScalaExtractor

//TODO: implement Compiler
class Driver(val source: String) {
  lazy val (bashSrc, scalaSrc) = ScalaExtractor.extractScala(source)


  //TODO: make outputDir a parameter
  val tempDirPrefix = "ctbash"
  lazy val outputDir = {
    val f = Files.createTempDirectory(tempDirPrefix)
    f.toFile.deleteOnExit()
    f
  }

}


