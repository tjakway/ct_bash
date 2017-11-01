package com.jakway.ctbash

import java.io.File
import java.nio.file.Files

import com.jakway.ctbash.compile.{BashCompiler, ExternalScalac}

object GeneralOptions {
  //TODO: check passed options

  def mkOptions(outputDir: Option[String], intermediateDir: Option[String], runMain: Boolean) = {
    //use the passed directories or create temp
    GeneralOptions(
      outputDir.map(new File(_)).getOrElse(outputDirName),
      intermediateDir.map(new File(_)).getOrElse(tempDir()),
      runMain,
      Array(),
      //by default, include the standard bash shebang
      BashCompiler.Options(Some(BashCompiler.Options.bashShebang)),
      ExternalScalac.Options(Array())
    )
  }

  val tempDirPrefix = "ctbash"

  val outputDirName = new File("bin")

  def tempDir(): File = {
    val f = Files.createTempDirectory(tempDirPrefix)
    f.toFile.deleteOnExit()
    f.toFile
  }
}

/**
  * whether to run a main method
  *
  * @param runMain
  */
case class GeneralOptions(outputDir: File,
                          intermediateDir: File,
                          runMain: Boolean,
                         //TODO: must be empty if runMain == false
                          mainArgs: Array[String],
                          bashOptions: BashCompiler.Options,
                          scalacOptions: ExternalScalac.Options)
