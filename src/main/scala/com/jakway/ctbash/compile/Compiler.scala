package com.jakway.ctbash.compile


trait CompileWarning {
  val description: String
}

/**
  * errors subclass warnings because warnings can be errors depending on circumstances
  * (e.g. -Werror=...)
  */
trait CompileError extends CompileWarning

case object NoExportedFields extends CompileWarning {
  override val description = "Your scala source doesn't export any fields, did you forget any @export annotations?"
}

sealed trait CompileOutput {
  def toEither[A]: Either[CompileFailed, CompileSuccess[A]] = CompileOutput.toEither(this)
}
class CompileSuccess[A](val warnings: Seq[CompileWarning], val output: A) extends CompileOutput
class CompileFailed(val why: Seq[CompileWarning]) extends CompileOutput

object CompileOutput {
  def toEither[A](c: CompileOutput): Either[CompileFailed, CompileSuccess[A]] = c match {
    case x: CompileSuccess[A] => Right(x)
    case y: CompileFailed => Left(y)
  }
}


trait CompilerOptions

trait Compiler[A <: CompilerOptions] {
  def compile(c: A): CompileOutput
}

