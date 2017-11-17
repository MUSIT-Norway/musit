package models.elasticsearch

import no.uio.musit.models.ActorId

case class ActorNames(m: Map[ActorId, String]) {
  def nameFor(id: ActorId): Option[String] = m.get(id)
}

object ActorNames {
  def apply(s: Set[(ActorId, String)]): ActorNames = ActorNames(s.toMap)
}
