package com.jakway.ctbash.compile

import com.jakway.ctbash.EvaluatedField

case class BashSource(src: String)

object BashCompiler {

  /**
    *
    * @param shebang whether to write a shebang line and if so what it should be
    */
  case class BashOptions(shebang: Option[String]) extends CompilerOptions

  object Options {
    val bashShebang = "#!/usr/bin/env bash"
  }
}

/**
  * _ <: Object ignores the parameterized type
  * see https://stackoverflow.com/questions/37232974/scala-how-to-completely-ignore-an-objects-type-parameter-when-needed
  *
  */
class BashCompiler(val evaluatedFields: Seq[EvaluatedField[_ <: Object]], val bashSource: BashSource)
  extends Compiler[BashCompiler.BashOptions] {
  val header = """"""

  // TODO: should error if multiple exported fields have the same name
  def compile(options: BashCompiler.BashOptions): CompileOutput = {
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
