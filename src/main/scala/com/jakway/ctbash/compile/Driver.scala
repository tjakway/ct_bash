package com.jakway.ctbash.compile

//TODO: implement Compiler
class Driver(val source: String) {
  lazy val (bashSrc, scalaSrc) = ScalaExtractor.extractScala(source)
}

object ScalaExtractor {
  val rgx = """(.*)@scala\s*{(.*)}(.*)""".r

  def hasScalaTag(src: String): Boolean =
    src.contains("@scala")

  /**
    *
    * @param src
    * @return Option[src] in case there isn't any of that source, in which case we can skip compilation
    */
  def extractScala(src: String): (Option[BashSource], Option[ScalaSource]) = {

    def helper(accBash: Seq[String], accScala: Seq[String])(p: String): (Seq[String], Seq[String]) = {
      rgx.findFirstMatchIn(p) match {
        case Some(m) => {
          //regex groups apparently count from 1, see https://stackoverflow.com/questions/3050907/scala-capture-group-using-regex
          val (before, scala, after) = (Option(m.group(1)), Option(m.group(2)), Option(m.group(3)))

          val newAccBash  = accBash  :+ before.getOrElse("")
          val newAccScala = accScala :+ scala.getOrElse("")

          val rest = after.getOrElse("")
          if(rest.isEmpty) {
            (newAccBash, newAccScala)
          }
          else {
            helper(newAccBash, newAccScala)(rest)
          }
        }
        case None => (accBash, accScala)
      }
    }

    val (bash, scala) = helper(Seq(), Seq())(src)

    def filterConcat[A](x: Seq[String])(f: String => A): Option[A] =
      Option(x)
        //return None if the Seq is empty
        .filter(!_.isEmpty)
        //join the sources with newlines
        .map(_.fold("")(_ + "\n" + _))
        //apply the constructor
        .map(f)


    (filterConcat(bash)(BashSource), filterConcat(scala)(ScalaSource))
  }
}
