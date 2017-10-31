package com.jakway.ctbash.parser

import scala.util.matching.Regex.Match

/**
  *
  * @param line line number, _starting from 1_
  * @param pos index _in that line_, starting from 1
  */
case class StringPosition(line: Int, pos: Int)

object StringPosition {

  case class StringPositionException(msg: String)
    extends Exception(msg)

  /**
    * newline is a String not a char because this varies by platform
    * \r, \n, \r\n, etc.
    */
  val newline: String = System.getProperty("line.separator")
  val newlineRegex = newline.r


  def matchesToStringPosition(source: String, ms: Iterator[Match], ranges: Option[Map[Int, (Int, Int)]] = None):
    Iterator[(StringPosition, StringPosition)] = {
    //the user can cache ranges by passing them explicitly
    ms.map(matchToStringPosition(ranges.getOrElse(lineNumberRanges(source))))
  }


  /**
    * @param ranges The result of calling lineNumberRanges, cached across calls
    * @param m
    * @return a tuple of the start and end of the match
    */
  def matchToStringPosition(ranges: Map[Int, (Int, Int)])(m: Match):
    (StringPosition, StringPosition) = {
    val l = lookupStringPosition(ranges)
    (for {
      start <- l(m.start)
      end <- l(m.end)
    } yield {
      (start, end)
    }) match {
      case Some((start, end)) => (start, end)
      case None => throw StringPositionException(s"Internal parser error: one or more lookups of positions (${m.start}, ${m.end}) in $ranges failed")
    }
  }

  def lookupStringPosition(ranges: Map[Int, (Int, Int)])(pos: Int):
    Option[StringPosition] = {
    ranges.find {
      case (_, (start, end)) => {
        pos >= start && pos < end
      }
    }.map {
      case (lineNo, (start, end)) => {
        //return the line number we found and the position
        //relative to the beginning of that line
        StringPosition(lineNo, pos - start)
      }
    }
  }


  /**
    *
    * @param s
    * @return line number -> (start, end)
    */
  def lineNumberRanges(s: String): Map[Int, (Int, Int)] = {
    var i: Int = 0;

    newlineRegex.findAllMatchIn(s).map {
      thisMatch => {
        val ret = i -> (thisMatch.start, thisMatch.end)
        i = i + 1 //i++
        ret
      }
    }.toMap
  }
}
