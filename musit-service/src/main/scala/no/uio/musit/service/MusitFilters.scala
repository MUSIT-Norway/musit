package no.uio.musit.service

import javax.inject.Inject
import play.api.http.DefaultHttpFilters

class MusitFilters @Inject() (f: NoCacheFilter) extends DefaultHttpFilters(f)
