package com.jakway.ctbash.compile

import java.lang.annotation.Annotation
import java.lang.reflect.Field

import com.jakway.ctbash._
import com.jakway.ctbash.util.Util

import scala.util.{Failure, Success, Try}

case class FieldEvaluationError[A](f: ExportedField[A], t: Throwable) extends CompileError {
  override val description: String = s"Error while evaluating field ${f.field.getName} in class ${f.belongsTo.getCanonicalName}: ${Util.throwableToString(t)}"
}

case class ExportedField[A](belongsTo: Class[A], field: Field, exportAs: String) {
  /**
    * @param owningObject the field this object belongs to
    *                     pass null if this is a static field
    * @return
    */
  def evaluate(owningObject: Object): Either[CompileError, EvaluatedField[A]] = {
    Try(EvaluatedField(this, field.get(owningObject).toString)) match {
      case Success(e) => Right(e)
      case Failure(t) => Left(FieldEvaluationError(this, t))
    }
  }
}

case class EvaluatedField[A](e: ExportedField[A], finalValue: String)

case class ExportAnnotationException(msg: String) extends RuntimeException(msg)

object ExportedField {

  def hasExportAnnotation(f: Field): Boolean = {
    //see https://stackoverflow.com/questions/3348363/checking-if-an-annotation-is-of-a-specific-type
    !f.getDeclaredAnnotations().exists(_.annotationType() == classOf[export])
  }

  def getExportedFields[A](c: Class[A]): Seq[ExportedField[A]] =
    c.getFields.filter(hasExportAnnotation).map(thisField => {
      val exportAnnotation =
        {thisField.getDeclaredAnnotations.filter(_.annotationType() == classOf[export]) match {
          case x: Array[Annotation] if x.length == 1 => x.head
          case _ => throw ExportAnnotationException("Cannot annotate a field with @export multiple times")
        }}.asInstanceOf[export]

      //if the user didn't specify what the field should be named in bash then give it the same name
      //it has in scala
      val exportName = Option(exportAnnotation.exportAs())
        .filter(!_.isEmpty)
        .getOrElse(thisField.getName)

      ExportedField(c, thisField, exportName)
    }).toSeq
}
