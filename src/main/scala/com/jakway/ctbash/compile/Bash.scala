package com.jakway.ctbash.compile

import com.jakway.ctbash.ExportedField

case class BashSource(src: String)

sealed trait CompileWarning {
  val description: String
}

/**
  * errors subclass warnings because warnings can be errors depending on circumstances
  * (e.g. -Werror=...)
  */
sealed trait CompileError extends CompileWarning



case object NoExportedFields extends CompileWarning {
  val description = "Your scala source doesn't export any fields, did you forget any @Export annotations?"
}

sealed trait CompileOutput
case class CompileSuccess(warnings: Seq[CompileWarning], output: String)
case class CompileFailed(why: Seq[CompileWarning])

/**
  * _ <: Object ignores the parameterized type
  * see https://stackoverflow.com/questions/37232974/scala-how-to-completely-ignore-an-objects-type-parameter-when-needed
  *
  * in the future probably add a CompileOptions parameter
  * for now, we're just writing variable names
  */
class Bash(val exportedFields: Seq[ExportedField[_ <: Object]], val bashSource: BashSource) {


  def compile(): CompileOutput = {
    ??? // TODO
  }
}
