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
  override val description = "Your scala source doesn't export any fields, did you forget any @Export annotations?"
}

sealed trait CompileOutput
case class CompileSuccess[A](warnings: Seq[CompileWarning], output: A) extends CompileOutput
case class CompileFailed(why: Seq[CompileWarning]) extends CompileOutput


trait CompilerOptions

trait Compiler[A <: CompilerOptions] {
  def compile(c: A): CompileOutput
}

