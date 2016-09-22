package no.uio.musit.microservice.storageAdmin.domain

import play.api.Logger
import play.api.libs.json._
import play.api.libs.json.Reads._

import scala.util.{Failure, Success, Try}


object Utils {
  def getExceptionMessageFromTry[T](myTry: Try[T]): Option[String] = {
    myTry match {
      case Success(_) => None
      case Failure(e) => Some(e.getMessage)
    }
  }
}

case class NodePath(path: String = ",") /*extends AnyVal*/ {

  val validationResult = NodePath.verifyValidFormat(path)
  require(validationResult.isSuccess, s"Invalid node path string $path. Error: ${Utils.getExceptionMessageFromTry(validationResult).getOrElse("WTF?")}")


  def parent: NodePath = {
    val stripped = path.stripSuffix(",")
    NodePath(stripped.substring(0, stripped.lastIndexOf(",") + 1))
  }

  def appendChild(childId: Long) = NodePath(s"$path$childId,")

  /* #OLD
  def serialize: String = {
    "," + (elements.foldLeft("") { (accum, element) => s"${element.toString()}, $accum" })
  }
  */
  def serialize = path

  def descendantsFilter = serialize + '%' //Assumes Oracle?
}


object NodePath {

  //  implicit val reads: Reads[NodePath] = __.read[String](NodePath.apply)

  // val logger = Logger(classOf[NodePath])

  def verifyValidFormat(path: String): Try[String] = {
    Try {
      if (!(path.startsWith(",") && path.endsWith(","))) {
        throw new Exception("A valid node path must start and end with a comma")
      }
      else {
        val split = path.trim.split(",").toList
        if (split.nonEmpty) {
          split.tail.foreach(s => s.toLong)
        }
      }
      path
    }
  }

  val empty = NodePath()
}
