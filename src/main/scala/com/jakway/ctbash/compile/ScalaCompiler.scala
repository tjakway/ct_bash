/**
 * This file contains code from CompilerMatcher.scala in scalaxb
 * Taken from https://github.com/eed3si9n/scalaxb/blob/b2f29c3e211deb752750c8311691e47b3f536081/integration/src/test/scala/CompilerMatcher.scala
 * License reproduced below:
 */
/*
 * Copyright (c) 2010 e.e d3si9n
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.jakway.ctbash.compile


import java.io.File

import org.slf4j.{Logger, LoggerFactory}

import scala.reflect.internal.util.Position
import scala.tools.nsc.{GenericRunnerSettings, Settings}
import scala.tools.nsc.reporters.AbstractReporter
import scala.collection.mutable

case class ScalaSource(src: String)

class OnCompilerWrite(val writeOp: String => Unit) {

  val msgs: mutable.Seq[String] = mutable.Seq()

  def write(s: String): Unit = {
    msgs.:+(s)
    writeOp(s)
  }
}

case class ScalacWarning(override val description: String) extends CompileWarning
case class ScalacError(override val description: String) extends CompileError


/**
  * translates scalac's AbstractReporter to slf4j logging
  * @param loggingClass
  * @param settings
  */
class LogReporter(val loggingClass: Class[_], override val settings: Settings) extends AbstractReporter {
  val logger: Logger = LoggerFactory.getLogger(loggingClass)

  val loggedWarnings: mutable.Seq[ScalacWarning] = mutable.Seq()
  val loggedErrors:   mutable.Seq[ScalacError]   = mutable.Seq()


  private def getLevel(severity: Severity): String => Unit = severity match {
        case INFO => logger.info _
        case WARNING => logger.warn _
        case ERROR   => logger.error _
        case _ => logger.info _
      }

  override def display(pos: Position, msg: String, severity: Severity): Unit = {
    val logFunction = getLevel(severity)
    logFunction(s"Msg@Position[$pos]: $msg")
  }

  override def displayPrompt(): Unit = {}

  override def error(pos: Position, msg: String): Unit = {
    super.error(pos, msg)
    loggedErrors.:+(ScalacError(msg))
  }

  override def warning(pos: Position, msg: String): Unit = {
    super.warning(pos, msg)
    loggedWarnings.:+(ScalacWarning(msg))
  }

}

object ScalaCompiler {
  import scala.tools.nsc.{Settings, GenericRunnerSettings}
  import scala.reflect.internal.util.{SourceFile, BatchSourceFile}
  import scala.tools.nsc.io.{PlainFile}
  import scala.tools.nsc.reporters.{ConsoleReporter}

  lazy val bootPathList = List(jarPathOfClass("scala.tools.nsc.Main"),
    jarPathOfClass("scala.Option"),
    jarPathOfClass("scala.xml.Elem"),
    jarPathOfClass("scala.util.parsing.combinator.Parsers"))

  // For some reason, there's a `java.net.URLClassLoader` in the
  // classloader hierarchy only for the first specs2 example in the suite.
  // This is most probably due to the sequential order of test execution, see
  // `testOptions in Test += Tests.Argument("sequential")` in `build.sbt`.
  // We assume that the current classpath doesn't change from example to
  // example in a single test suite.
  lazy val currentcp = {
    val currentLoader = java.lang.Thread.currentThread.getContextClassLoader
    currentLoader match {
      case cl: java.net.URLClassLoader => cl.getURLs.toList map {_.toString}
      case x =>
        // sbt 0.13 wraps classloader with ClasspathFilter
        x.getParent match {
          case cl: java.net.URLClassLoader => cl.getURLs.toList map {_.toString}
          case x => sys.error("classloader is not a URLClassLoader: " + x.getClass)
        }
    }
  }

  def settings(writer: String => Unit)(outdir: String, classpath: List[String],
                       usecurrentcp: Boolean, unchecked: Boolean,
                       deprecation: Boolean, feature: Boolean, fatalWarnings: Boolean): GenericRunnerSettings = {
    import java.io.{PrintWriter, BufferedWriter, BufferedReader, StringReader, OutputStreamWriter}

    val classpathList = classpath ++ (if (usecurrentcp) currentcp else Nil)
    val in = new BufferedReader(new StringReader(""))
    val grs = new GenericRunnerSettings(writer)
    val origBootclasspath = grs.bootclasspath.value

    grs.bootclasspath.value =
      mkClasspath(origBootclasspath :: bootPathList)

    val originalClasspath = grs.classpath.value
    grs.classpath.value = mkClasspath(classpathList)
    grs.outdir.value = outdir
    grs.unchecked.value = unchecked
    grs.deprecation.value = deprecation
    grs.feature.value = feature
    grs.fatalWarnings.value = fatalWarnings
    grs
  }

  def copyFileFromResource(source: String, dest: File) {
    val in = getClass.getResourceAsStream(source)
    val reader = new java.io.BufferedReader(new java.io.InputStreamReader(in))
    val out = new java.io.PrintWriter(new java.io.FileWriter(dest))
    var line: String = null
    line = reader.readLine
    while (line != null) {
      out.println(line)
      line = reader.readLine
    }
    in.close
    out.flush
  }
  private def jarPathOfClass(className: String) = {
    val resource = className.split('.').mkString("/", "/", ".class")
    val path = getClass.getResource(resource).getPath
    val indexOfFile = path.indexOf("file:")
    val indexOfSeparator = path.lastIndexOf('!')
    if (indexOfFile == -1 || indexOfSeparator == -1) {
      val indexOfSlash = path.lastIndexOf('/')
      path.substring(0, indexOfSlash)
    } else {
      path.substring(indexOfFile, indexOfSeparator)
    }
  }
  private def toSourceFile(file: File): SourceFile =
    new BatchSourceFile(new PlainFile(file))


  private def mkClasspath(entries:List[String]):String = {
    def windowsFix(path:String):String =
      if(java.io.File.separatorChar != '\\') path
      else { // Windows
        (if(path.startsWith("file:")) path.substring(6) else path)
          .replace('/', java.io.File.separatorChar)
      }

    entries.distinct
      .map(windowsFix)
      .mkString(java.io.File.pathSeparator)
  }


  case class ScalaOptions(outdir: String = ".",
                          classpath: List[String] = Nil,
                          usecurrentcp: Boolean = false,
                          unchecked: Boolean = true,
                          deprecation: Boolean = true,
                          feature: Boolean = true,
                          fatalWarnings: Boolean = true) extends CompilerOptions {
    def toSettings = {
      settings(println _)(outdir, classpath, usecurrentcp, unchecked,
        deprecation, feature, fatalWarnings)
    }
  }
}

class ScalaCompiler(val filesToCompile: Seq[File]) extends Compiler[ScalaCompiler.ScalaOptions] {
  import ScalaCompiler._

  val logger: Logger = LoggerFactory.getLogger(getClass())

  val stdOutWriter = new OnCompilerWrite(println)

  override def compile(scalaOptions: ScalaOptions): CompileOutput = {

    import scala.tools.nsc.{Global}

    val s = scalaOptions.toSettings
    val reporter = new LogReporter(getClass(), s)
    val compiler = new Global(s, reporter)
    val run = (new compiler.Run)
    run.compile(filesToCompile.map(_.getAbsolutePath).toList)

    if(reporter.hasErrors) {
      logger.error(s"$filesToCompile failed to compile")
      new CompileFailed(reporter.loggedWarnings ++ reporter.loggedErrors)
    } else {
      logger.debug(s"$filesToCompile compiled")
      new CompileSuccess(reporter.loggedWarnings, run)
    }
  }
}
