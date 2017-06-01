package no.uio.musit.models

case class PagedResult[A](totalMatches: Int, matches: Seq[A])
