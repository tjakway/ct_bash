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
import scala.tools.nsc.Settings
import scala.tools.nsc.reporters.AbstractReporter

import scala.collection.mutable

case class ScalaSource(src: String)

// TODO
//case class ScalaOptions extends CompilerOptions

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

class ScalaCompiler extends Compiler[CompilerOptions] {
  import scala.tools.nsc.{Settings, GenericRunnerSettings}
  import scala.reflect.internal.util.{SourceFile, BatchSourceFile}
  import scala.tools.nsc.io.{PlainFile}
  import scala.tools.nsc.reporters.{ConsoleReporter}

  val logger: Logger = LoggerFactory.getLogger(getClass())

  val stdOutWriter = new OnCompilerWrite(println)


  private lazy val bootPathList = List(jarPathOfClass("scala.tools.nsc.Main"),
                                   jarPathOfClass("scala.Option"),
                                   jarPathOfClass("scala.xml.Elem"),
                                   jarPathOfClass("scala.util.parsing.combinator.Parsers"))

  // For some reason, there's a `java.net.URLClassLoader` in the
  // classloader hierarchy only for the first specs2 example in the suite.
  // This is most probably due to the sequential order of test execution, see
  // `testOptions in Test += Tests.Argument("sequential")` in `build.sbt`.
  // We assume that the current classpath doesn't change from example to
  // example in a single test suite.
  private lazy val currentcp = {
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

  /** evaluteTo matches a pair of code list and files against the expected value
   * after evaluating the files and the given code list.
   * @param expected: Any - expected value
   * @param outdir: String - output dir for the interpreter
   *
   * <code>
   * (List("import ipo._",
   *       "Address(\"\", \"\", \"\").toString"),
   *   List(generated)) must evaluateTo("Address(,,)")
   * </code>
   */
  def evaluateTo(expected: Any,
      outdir: String = ".",
      classpath: List[String] = Nil,
      usecurrentcp: Boolean = false,
      unchecked: Boolean = true,
      deprecation: Boolean = true,
      feature: Boolean = true,
      fatalWarnings: Boolean = true) = new Matcher[(Seq[String], Seq[File])] {

    def apply[A <: (Seq[String], Seq[File])](pair: Expectable[A]) = {
      import scala.tools.nsc.interpreter.{IMain, Results => IR}
      import scala.util.control.Exception._

      val code = pair.value._1
      val files = pair.value._2
      if (code.size < 1)
        sys.error("At least one line of code is required.")
      val s = settings(outdir, classpath, usecurrentcp, unchecked,
        deprecation, feature, fatalWarnings)
      val main = new IMain(s) {
        def lastReq = prevRequestList.last
      }
      if (!main.compileSources(files.map(toSourceFile(_)): _*)) {
        sys.error(s"""Error compiling: ${ files.mkString(",") }""")
      }
      code foreach { c => main.interpret(c) match {
        case IR.Error => sys.error("Error interpreting %s" format (c))
        case _ =>
      }}
      val holder0 = allCatch opt {
        main.lastReq.lineRep.call("$result")
      }

      def mitigate(s: String): String = {
        // ListMap#toString went from "Map" (2.11) to "ListMap" (2.12), switch it back
        s.replace("ListMap", "Map")
      }

      val holder = holder0 map {
        case s: String => mitigate(s)
        case x         => x
      }

      if (holder != Some(expected)) {
        val actual = holder.fold("None")(_.toString)
        val actualClass = holder.fold("<none>")(_.getClass.toString)
        println(s"  actual: $actual ($actualClass)")
        println(s"expected: $expected (${expected.getClass})")
      }
      result(holder == Some(expected),
        code + " evaluates as expected",
        code + " does not evaluate as expected",
        pair)
    }
  }

  private def settings(outdir: String, classpath: List[String],
      usecurrentcp: Boolean, unchecked: Boolean,
      deprecation: Boolean, feature: Boolean, fatalWarnings: Boolean): GenericRunnerSettings = {
    import java.io.{PrintWriter, BufferedWriter, BufferedReader, StringReader, OutputStreamWriter}

    val classpathList = classpath ++ (if (usecurrentcp) currentcp else Nil)
    val in = new BufferedReader(new StringReader(""))
    val grs = new GenericRunnerSettings(stdOutWriter.write)
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

  def compile(outdir: String = ".",
    classpath: List[String] = Nil,
    usecurrentcp: Boolean = false,
    unchecked: Boolean = true,
    deprecation: Boolean = true,
    feature: Boolean = true,
    fatalWarnings: Boolean = true)(files: Seq[File]): CompileOutput = {

    import scala.tools.nsc.{Global}

    val s = settings(outdir, classpath, usecurrentcp, unchecked,
      deprecation, feature, fatalWarnings)
    val reporter = new LogReporter(getClass(), s)
    val compiler = new Global(s, reporter)
    val run = (new compiler.Run)
    run.compile(files.map(_.getAbsolutePath).toList)

    if(reporter.hasErrors) {
      logger.error(s"$files failed to compile")
      CompileFailed(reporter.loggedWarnings ++ reporter.loggedErrors)
    } else {
      logger.debug(s"$files compiled")
      CompileSuccess(reporter.loggedWarnings, run)
    }
  }

  def deleteAll(file: File): Boolean = {
    if (file.isDirectory) {
      val children = file.listFiles
      if (children != null)
        children.foreach(deleteAll(_))
    }
    file.delete
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



  override def compile(c: CompilerOptions) = {
    //TODO
    ???
  }
}
