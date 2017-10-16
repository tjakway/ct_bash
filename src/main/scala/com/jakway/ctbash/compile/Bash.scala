package com.jakway.ctbash.compile

import com.jakway.ctbash.{EvaluatedField, ExportedField}

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
case class CompileSuccess(warnings: Seq[CompileWarning], output: String) extends CompileOutput
case class CompileFailed(why: Seq[CompileWarning]) extends CompileOutput


trait Compiler {
  val header: String
  def compile(c: Compiler.Options): CompileOutput
}

object Compiler {

  /**
    *
    * @param shebang whether to write a shebang line and if so what it should be
    */
  case class Options(shebang: Option[String])

  object Options {
    val bashShebang = "#!/usr/bin/env bash"
  }
}

/**
  * _ <: Object ignores the parameterized type
  * see https://stackoverflow.com/questions/37232974/scala-how-to-completely-ignore-an-objects-type-parameter-when-needed
  *
  */
class BashCompiler(val evaluatedFields: Seq[EvaluatedField[_ <: Object]], val bashSource: BashSource) extends Compiler {
  val header = """"""

  // TODO: should error if multiple exported fields have the same name
  def compile(options: Compiler.Options): CompileOutput = {
    val warnings = if(evaluatedFields.isEmpty) {
      Seq(NoExportedFields)
    } else {
      Seq()
    }

    val fieldStr = evaluatedFields.map {
      case EvaluatedField(ExportedField(_, _, exportAs), finalValue) => {
        //TODO: check for any characters in exportAs or finalValue that have to be escaped
        exportAs + "=" + finalValue
      }
        //join w/newlines
    }.fold("")(_ + "\n" + _)

    val output = header + "\n" + fieldStr + bashSource
    CompileSuccess(warnings, output)
  }
}
