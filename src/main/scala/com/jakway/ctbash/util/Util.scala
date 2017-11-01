package com.jakway.ctbash.util

import java.io.{File, PrintWriter, StringWriter}

import org.slf4j.Logger

import scala.util.{Failure, Success, Try}
import scalaz.Alpha.{E, T}
import scalaz.Monoid

object Util {
  def collapseEithers[T](in: Seq[Either[String, T]]): Either[String, Seq[T]] = {
    in.foldLeft(Right(Seq()): Either[String, Seq[T]]) { (res, it) =>
      (res, it) match {
        //short circuit on error
        case (Left(msg), _) => Left(msg)
        case (Right(_), Left(msg)) => Left(msg)

        case (Right(group), Right(a)) => Right(group :+ a)
      }
    }
  }

  def accumulateEithers[E: Monoid, T: Monoid](in: Seq[Either[E, T]]):
    Either[E, T] = {
    import scalaz._
    import scalaz.syntax.monoid._

    val empty: (E, T) = (Monoid[E].zero, Monoid[T].zero)

    val (errsAcc, resAcc) = in.foldLeft(empty) {
      //accumulate errors
      case ((errsAcc, resAcc), Left(elem)) => {
        (errsAcc mappend elem, resAcc)
      }
        //if we haven't found an error, accumulate Right elements
      case ((errsAcc, resAcc), Right(elem))
        if errsAcc.isMZero => {
        (errsAcc, resAcc.mappend(elem))
      }
        //if we've found an error, skip the rest of the Right elements
      case ((errsAcc, resAcc), Right(elem))
        if !errsAcc.isMZero => {
        (errsAcc, resAcc)
      }
    }

    //return accumulated errors if there are any
    if(!errsAcc.isMZero) {
      Left(errsAcc)
      //otherwise return accumulated elements
    } else {
      Right(resAcc)
    }
  }

  def eitherToTry[A](e: String => Exception)(f: () => Either[String, A]): Try[A] =
    Try {
      f() match {
        case Left(msg) => throw e(msg)
        case Right(x) => x
      }
    }

  /**
    * returns a function that runs the passed function and converts any exceptions it throws
    * to Lefts
    * @param f
    * @tparam A
    * @tparam B
    * @return
    */
  def exceptionToEither[A, B](f: A => Either[String, B]): A => Either[String, B] = {
    try {
      f
    }
    catch {
      case t: Throwable => (_ => Left(t.toString))
    }
  }

  def tryToEither[A](t: Try[A]): Either[String, A] = t match {
    case Success(value) => Right(value)
    case Failure(exception) => Left(Util.throwableToString(exception))
  }

  def tryToOptionLogException[A](logger: Logger)(t: Try[A]): Option[A] = t match {
    case Success(value) => Some(value)
    case Failure(exception) => {
      logger.debug("", exception)
      None
    }
  }

  /**
    * see https://stackoverflow.com/questions/1149703/how-can-i-convert-a-stack-trace-to-a-string
    * @return
    */
  def throwableToString(t: Throwable, maxLines: Int = 15) = {
    val sw: StringWriter = new StringWriter()
    val pw: PrintWriter  = new PrintWriter(sw)
    t.printStackTrace(pw)
    sw.toString().lines.take(maxLines).mkString("\n") // stack trace as a string
  }

  def mkTempFile(prefix: String): Either[String, File] = {
    for {
      tempFile <- tryToEither(Try(File.createTempFile(prefix, null))).right
      _ <- tryToEither(Try(tempFile.deleteOnExit)).right
    } yield {
      tempFile
    }
  }

  def combineExceptions(newest: Throwable, oldest: Throwable): Throwable =
    new Throwable(newest.getMessage, oldest.getCause)

  def removeFilenameExtension(fname: String): String = {
    fname.substring(0, fname.lastIndexOf('.'))
  }
}

