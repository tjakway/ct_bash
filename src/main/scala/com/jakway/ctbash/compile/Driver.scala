package com.jakway.ctbash.compile

import java.io.File
import java.nio.file.Files

import com.jakway.ctbash.GeneralOptions
import com.jakway.ctbash.parser.{ParserError, ScalaExtractor}
import com.jakway.ctbash.util.Util

import scala.util.Try

//TODO: implement Compiler
class Driver(val opts: GeneralOptions, val sourceFiles: Vector[File]) {
  val sources: Vector[String] = sourceFiles.map(scala.io.Source.fromFile(_).mkString)

  def compileScala(srcs: Vector[(Option[BashSource], Option[ScalaSource])]) = {
    val scalaSources = srcs.map(_._2).flatMap(_.toVector)
    new ExternalScalac(scalaSources, opts.intermediateDir, Seq())
      .compile(opts.scalacOptions)
  }

  def compileBash(srcs: Vector[(Option[BashSource], Option[ScalaSource])],evaluatedFields: Vector[EvaluatedField[_]]) = {
    val bashSources = srcs.map(_._1).flatMap(_.toVector)

    new BashCompiler(evaluatedFields, bashSources)
      .compile(opts.bashOptions)
  }

  def runMain(executor: Executor) = {
    if(opts.runMain) {
      executor.evaluateMain(opts.mainArgs)
    } else {
      Right("")
    }
  }

  def run() = {
    //TODO: resolve confusion of Seq[CompilerError] vs. Vector[CompilerError]
    for {
      srcs <- ScalaExtractor.extractScala(sources)
      compiledScala <- compileScala(srcs).toEither[File].right
      (classLoader, classFiles) <- Util.tryToEither(
                                      Try(Loader.loadAllClassFiles(compiledScala.output)))

      executor = new Executor(classFiles)
      stdout <- runMain(executor)
      evaluatedFields <- executor.evaluateFields()

      compiledBash <- compileBash(srcs, evaluatedFields)

    } yield {
      ???
    }
  }
}
