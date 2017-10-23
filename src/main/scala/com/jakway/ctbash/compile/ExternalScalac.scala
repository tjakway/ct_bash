package com.jakway.ctbash.compile

import java.io.File
import java.nio.file.Files

/**
  *
  * @param scalaOutputDir where to put the generated scala files
  *                       a temp dir if None
  * @param scalacArgs
  */
case class ExternalScalacOptions(scalaOutputDir: Option[File],
                                 scalacArgs: Array[String]) extends CompilerOptions

class ExternalScalac(val filesToCompile: Seq[File], extraArgs: Seq[String])
  extends Compiler[ExternalScalacOptions] {

  val tempDirPrefix = "ctbash"

  lazy val outputDir = {
    val f = Files.createTempDirectory(tempDirPrefix)
    f.toFile.deleteOnExit()
    f
  }

  lazy val builtinOptions = Seq[String]("-d", outputDir.toAbsolutePath.toString)

  override def compile(passedOpts: ExternalScalacOptions) = {
    import scala.sys.process

    val options: Array[String] = (builtinOptions ++ passedOpts.scalacArgs).toArray
    val scalaOutputDir = passedOpts.scalaOutputDir.getOrElse(outputDir)

    /** TODO: process */

    val scalac =

  }
}
