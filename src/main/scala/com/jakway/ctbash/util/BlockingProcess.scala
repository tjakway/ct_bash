package com.jakway.ctbash.util

object BlockingProcess {
  case class Result(exitCode: Int, stdout: String, stderr: String)
}

class BlockingProcess(name: String, args: Seq[String]) {
  import BlockingProcess._

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

