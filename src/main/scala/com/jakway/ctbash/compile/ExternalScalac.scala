package com.jakway.ctbash.compile

import java.io.File

import com.jakway.ctbash.util.BlockingProcess
import org.slf4j.{Logger, LoggerFactory}


object ExternalScalac {
  class ScalacOutput(stdout: String, stderr: String) extends CompileWarning {
    override val description: String = s"stdout: $stdout, stderr: $stderr"
  }

  case class ScalacFailed(exitCode: Int, stdout: String, stderr: String)
    extends CompileFailed(Seq(new ScalacOutput(stdout, stderr)))

  case class ScalacSuccess(outputDir: File, stdout: String, stderr: String)
    extends CompileSuccess[File](Seq(new ScalacOutput(stdout, stderr)), outputDir)

  /**
    *
    * @param scalacArgs
    */
  case class Options(scalacArgs: Array[String]) extends CompilerOptions
}



//TODO: remove extraArgs since we pass them in compile()
class ExternalScalac(val filesToCompile: Seq[ScalaSource], val outputDir: File, extraArgs: Seq[String])
  extends Compiler[ExternalScalac.Options] {
  import BlockingProcess.Result
  import ExternalScalac._

  val logger: Logger = LoggerFactory.getLogger(getClass())


  lazy val builtinOptions = Seq[String]("-d", outputDir.getAbsolutePath)
  val srcs = filesToCompile.map(_.src).map(f => new File(f).getAbsolutePath)

  override def compile(passedOpts: ExternalScalac.Options): CompileOutput = {
    val options: Array[String] =
      (builtinOptions ++ passedOpts.scalacArgs ++ srcs).toArray

    handleScalacRun(new BlockingProcess("scalac", options).run())
  }

  def handleScalacRun(res: BlockingProcess.Result): CompileOutput = {
    def logStreams(f: String => Unit) = {
      f(s"stdout: ${res.stdout}")
      f(s"stderr: ${res.stderr}")
    }

    res match {
      case Result(0, _, _) => {
        logger.debug("scalac invocation successful")
        logStreams(logger.debug)

        ScalacSuccess(outputDir, res.stdout, res.stderr)
      }

      case Result(exitCode, _, _) => {
        logger.error(s"scalac invocation failed with exit code $exitCode")
        logStreams(logger.error)
        ScalacFailed(exitCode, res.stdout, res.stderr)
      }
    }
  }
}
