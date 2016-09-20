package no.uio.musit.microservice.storageAdmin.domain

import scala.util.Try


/**
  * Created by jarle on 20.09.16.
  */
case class NodePath(elements: List[Long]) extends AnyVal {

  def serialize: String = {
    "," + (elements.map { element => s"${element.toString()}," }.fold("")(_ + _))
  }
  def descendantsFilter = serialize + '%' //Assumes Oracle?
}


object NodePath {

  def tryParse(nodePathAsString: String): Option[List[Long]] = {

    /** Throws exception if syntax error! */
    def parse(nodePathAsString: String) = {
      if (nodePathAsString == "") //We need to do this check because "".split(",") returns an array of length 1, not 0!
      {
        List.empty
      }
      else {
        nodePathAsString.split(",").map { intAsString => intAsString.toLong }.toList
      }
    }

    Try(parse(nodePathAsString)).toOption
  }


  def apply(nodePathAsString: String): NodePath = {
    val optResult = tryParse(nodePathAsString)
    require(optResult.isDefined)

    NodePath(optResult.get)
  }

  val empty = NodePath(List.empty)

}
