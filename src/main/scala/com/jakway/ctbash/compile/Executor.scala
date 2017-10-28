package com.jakway.ctbash.compile

import java.io.{File, FileInputStream, FilenameFilter}
import java.lang.reflect.Method
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.{Files, Path}

import com.jakway.ctbash.ExportedField

class Executor(val classFiles: Seq[Class[_]]) {
  import java.lang.reflect.Modifier

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
      m.getReturnType == Void &&
      //that take 1 parameter
      m.getParameterCount == 1 &&
      //take an Array[String]
      checkFirstArgType(m) &&
      //and is named main
      m.getName == "main"
  }

  def findMains(): Map[Class[_], Array[Method]] = {
    val map = classFiles.map { c =>
      (c, c.getDeclaredMethods())
    }.toMap

    map.mapValues(ms => ms.filter(isMain))
      //remove empty members
      .filter(!_._2.isEmpty)
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
