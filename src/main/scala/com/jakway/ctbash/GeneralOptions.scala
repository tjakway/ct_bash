package com.jakway.ctbash

import java.io.File
import java.nio.file.Files

import com.jakway.ctbash.compile.ExternalScalac

object GeneralOptions {
  //TODO: check passed options

  def mkOptions(outputDir: Option[String], intermediateDir: Option[String], runMain: Boolean) = {
    //use the passed directories or create temp
    GeneralOptions(
      outputDir.map(new File(_)).getOrElse(outputDirName),
      intermediateDir.map(new File(_)).getOrElse(tempDir()),
      runMain,
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
                          scalacOptions: ExternalScalac.Options)
