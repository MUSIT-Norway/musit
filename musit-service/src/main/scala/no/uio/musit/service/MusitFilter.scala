package no.uio.musit.service

import javax.inject.Inject
import play.api.http.DefaultHttpFilters

class MusitFilter @Inject()(
    noCache: NoCacheFilter,
    logging: AccessLogFilter
) extends DefaultHttpFilters(noCache, logging)
