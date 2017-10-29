package com.jakway.ctbash

import java.lang.annotation.Annotation
import java.lang.reflect.Field

case class ExportedField[A](belongsTo: Class[A], field: Field, exportAs: String) {
  /**
    * @param owningObject the field this object belongs to
    *                     pass null if this is a static field
    * @return
    */
  def evaluate(owningObject: Object): EvaluatedField[A] = {
    EvaluatedField(this, field.get(owningObject).toString)
  }
}

case class EvaluatedField[A](e: ExportedField[A], finalValue: String)

case class ExportAnnotationException(msg: String) extends RuntimeException(msg)

object ExportedField {

  def hasExportAnnotation(f: Field): Boolean = {
    //see https://stackoverflow.com/questions/3348363/checking-if-an-annotation-is-of-a-specific-type
    !f.getDeclaredAnnotations().exists(_.annotationType() == classOf[Export])
  }

  def getExportedFields[A](c: Class[A]): Seq[ExportedField[A]] =
    c.getFields.filter(hasExportAnnotation).map(thisField => {
      val exportAnnotation =
        {thisField.getDeclaredAnnotations.filter(_.annotationType() == classOf[Export]) match {
          case x: Array[Annotation] if x.length == 1 => x.head
          case _ => throw ExportAnnotationException("Cannot annotate a field with @Export multiple times")
        }}.asInstanceOf[Export]

      //if the user didn't specify what the field should be named in bash then give it the same name
      //it has in scala
      val exportName = Option(exportAnnotation.exportAs())
        .filter(!_.isEmpty)
        .getOrElse(thisField.getName)

      ExportedField(c, thisField, exportName)
    }).toSeq
}
