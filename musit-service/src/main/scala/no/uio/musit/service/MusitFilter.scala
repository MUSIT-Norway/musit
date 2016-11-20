package no.uio.musit.service

import javax.inject.Inject
import play.api.http.DefaultHttpFilters

class MusitFilter @Inject() (f: NoCacheFilter) extends DefaultHttpFilters(f)
