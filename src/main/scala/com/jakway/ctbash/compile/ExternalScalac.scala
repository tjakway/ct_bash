package com.jakway.ctbash.compile

import java.io.File
import java.nio.file.Files

class BlockingProcess(name: String, args: Seq[String]) {
  case class Result(exitCode: Int, stdout: String, stderr: String)

  def run(): Result = {
    import scala.sys.process.{Process, ProcessLogger}
    var stdout = ""
    var stderr = ""
    val proc = Process(Seq(name) ++ args)
    val procLogger = ProcessLogger(line => stdout += (line + "\n"),
      line => stderr += (line + "\n"))

    val exitCode: Int = proc.run(procLogger).exitValue()

    Result(exitCode, stdout, stderr)
  }
}


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
