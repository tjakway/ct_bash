package com.jakway.ctbash.compile

import java.io.File

import com.jakway.ctbash.util.BlockingProcess



/**
  *
  * @param scalacArgs
  */
case class ExternalScalacOptions(scalacArgs: Array[String]) extends CompilerOptions

class ExternalScalac(val filesToCompile: Seq[ScalaSource], val outputDir: File, extraArgs: Seq[String])
  extends Compiler[ExternalScalacOptions] {


  lazy val builtinOptions = Seq[String]("-d", outputDir.getAbsolutePath)

  override def compile(passedOpts: ExternalScalacOptions) = {
    val options: Array[String] = (builtinOptions ++ passedOpts.scalacArgs).toArray

    val res = new BlockingProcess("scalac", options).run()
  }
}
