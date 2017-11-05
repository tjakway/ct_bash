package com.jakway.ctbash.compile

case class BashSource(src: String)

object BashCompiler {

  /**
    *
    * @param shebang whether to write a shebang line and if so what it should be
    */
  case class Options(shebang: Option[String]) extends CompilerOptions

  object Options {
    val bashShebang = "#!/usr/bin/env bash"
  }
}

//TODO: handle multiple bash sources
class BashCompiler(val evaluatedFields: Vector[EvaluatedField[_]], val bashSource: BashSource)
  extends Compiler[BashCompiler.Options] {
  val header = """"""

  // TODO: should error if multiple exported fields have the same name
  def compile(options: BashCompiler.Options): CompileOutput = {
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
      new CompileSuccess(warnings, output)
  }
}
