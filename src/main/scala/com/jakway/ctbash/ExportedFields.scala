package com.jakway.ctbash

import java.lang.reflect.Field

case class ExportedField[A](belongsTo: Class[A], field: Field)

class ExportedFields {
  def hasExportAnnotation(f: Field): Boolean = {
    !f.getDeclaredAnnotations().filter(_.annotationType().isInstanceOf[Export]).isEmpty
  }

  def getExportedFields[A](c: Class[A]): Seq[ExportedField[A]] =
    c.getFields.filter(hasExportAnnotation).map(ExportedField(c, _)).toSeq
}
