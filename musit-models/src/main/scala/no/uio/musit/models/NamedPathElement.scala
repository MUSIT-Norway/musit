package no.uio.musit.models

import play.api.libs.json.{Format, Json}

/**
 * A NodePath contains a comma separated String of {{{StorageNodeId}}} (or Long)
 * values, each of these ID's can be represented as a {{{NamedPathElement}}}.
 *
 * @param nodeId {{{StorageNodeDatabaseId}}} of the named path element
 * @param nodeUuid {{{StorageNodeId}}} of the named path element
 * @param name     {{{String}}} containing the name value of the StorageNode.
 */
case class NamedPathElement(
    nodeId: StorageNodeDatabaseId,
    nodeUuid: StorageNodeId,
    name: String
)

object NamedPathElement {

  implicit val formats: Format[NamedPathElement] = Json.format[NamedPathElement]

}
