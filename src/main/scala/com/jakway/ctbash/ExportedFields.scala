package com.jakway.ctbash

import java.lang.reflect.Field

case class ExportedField[A](belongsTo: Class[A], field: Field, exportAs: String)

case class EvaluatedField[A](e: ExportedField[A], finalValue: String)

case class ExportAnnotationException(msg: String) extends RuntimeException(msg)

object ExportedField {

  def hasExportAnnotation(f: Field): Boolean = {
    !f.getDeclaredAnnotations().filter(_.annotationType().isInstanceOf[Export]).isEmpty
  }

  def getExportedFields[A](c: Class[A]): Seq[ExportedField[A]] =
    c.getFields.filter(hasExportAnnotation).map(thisField => {
      val exportAnnotation =
        {thisField.getDeclaredAnnotations.filter(_.annotationType().isInstanceOf[Export]) match {
          case x@Seq(a) if x.length == 1 => a
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
