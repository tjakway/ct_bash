package com.jakway.ctbash.compile

import java.io.{File, FileInputStream, FilenameFilter}
import java.lang.reflect.Method
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.{Files, Path}

import com.jakway.ctbash.ExportedField
import org.slf4j.{Logger, LoggerFactory}

case class MultipleMainMethods(c: Class[_]) extends CompileError {
  override val description =
    s"Multiple main methods found, one belongs to ${c.getCanonicalName}."
}

case object NoMainMethod extends CompileError {
  override val description: String = "No main method found."
}

/**
  * reflection is expensive, use sparingly
  * @param classFiles
  */
class Executor(val classFiles: Seq[Class[_]]) {
  import java.lang.reflect.Modifier

  val logger: Logger = LoggerFactory.getLogger(getClass())

  val exportedFields: Map[Class[_], Seq[ExportedField[_]]] = {
    classFiles.map { c =>
      (c, ExportedField.getExportedFields(c))
    }.toMap
  }

  private def checkFirstArgType(m: Method): Boolean = {
    val firstArg = m.getParameterTypes.head
    val h = firstArg.getTypeParameters

    //check that the first argument of the method is an Array[String]
    firstArg.isArray &&
    //should only be 1 type parameter
    h.length == 1 &&
      //make sure the type parameter is String
      h.headOption.filter(_.getName == "java.lang.String").isDefined
  }

  private def isMain(m: Method): Boolean = {
    //find public static methods
    Modifier.isPublic(m.getModifiers) &&
      Modifier.isStatic(m.getModifiers) &&
      //that return void
      m.getReturnType == Void.TYPE &&
      //that take 1 parameter
      m.getParameterCount == 1 &&
      //take an Array[String]
      checkFirstArgType(m) &&
      //and is named main
      m.getName == "main"
  }

  private def foldFindMain(acc: Either[Seq[CompileError], Option[Method]], c: Class[_]):
    Either[Seq[CompileError], Option[Method]] = {

    lazy val mainMethods = c.getDeclaredMethods().filter(isMain)

    acc match {
        //if we already found errors keep going to see if we can find multiple main methods
      case Left(errs) => {
        if(mainMethods.length > 0 && errs.exists(_.isInstanceOf[MultipleMainMethods])) {
          Left(errs ++ Seq(MultipleMainMethods(c)))
        } else {
          Left(errs)
        }
      }
        //if we've already found a main method we shouldn't find another
      case Right(Some(m)) if mainMethods.length > 0 => {
        Left(Seq(MultipleMainMethods(m.getDeclaringClass), MultipleMainMethods(c)))
      }
        //finding >1 main method in a single class is always an error
      case _ if mainMethods.length > 1 => {
        //should never happen
        logger.error(s"Multiple main methods declared in class ${c.getCanonicalName}--should never happen")
        Left(mainMethods.map(_ => MultipleMainMethods(c)))
      }
        //otherwise check if we've found a main method
      case Right(None) => {
        Right(mainMethods.headOption)
      }
    }
  }

  def findMain(): Either[Seq[CompileError], Method] = {
    classFiles.foldLeft(Right(None))(foldFindMain) match {
      case Right(None) => Left(Seq(NoMainMethod))
      case Right(Some(m)) => Right(m)
      case Left(a) => Left(a)
    }
  }
}


class Loader extends ClassLoader {

  def loadClassFromFile[_](file: File): Class[_] = {
    val bytes = Files.readAllBytes(file.toPath)

    defineClass(null, bytes, 0, bytes.length)
  }
}

object Loader {
  val classFileFilter = new FilenameFilter {
    override def accept(dir: File, name: String): Boolean = name.endsWith(".class")
  }

  def getAllClassFiles(dir: File): Seq[File] = {
    dir.listFiles(classFileFilter).flatMap { f =>
      //recurse over subdirectories
      if(f.isDirectory) {
        getAllClassFiles(f)
      } else {
        Seq(f)
      }
    }
  }

  def loadAllClassFiles(outputDir: File): (ClassLoader, Seq[Class[_]]) = {
    val allClassFiles = getAllClassFiles(outputDir)
    val loader = new Loader()

    (loader, allClassFiles.map(loader.loadClassFromFile))
  }
}
