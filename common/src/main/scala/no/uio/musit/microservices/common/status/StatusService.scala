/*
 *  MUSIT is a museum database to archive natural and cultural history data.
 *  Copyright (C) 2016  MUSIT Norway, part of www.uio.no (University of Oslo)
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License,
 *  or any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package no.uio.musit.microservices.common.status

import play.api.mvc._
import io.swagger.annotations._

@Api(value = "/api/status", description = "Status service")
class StatusService extends Controller {

  @ApiOperation(
    value = "Status operation - Show simple fast status, made for monitoring.",
    notes = "Ment to be used for machine monitoring.",
    httpMethod = "GET"
  )
  def brief_status = Action {
    Ok("Status has not been implemented.")
  }

  @ApiOperation(
    value = "Extended status operation - Show an extended status to ease checking the internals.",
    notes = "Ment to be used for diagnostic.",
    httpMethod = "GET"
  )
  def extended_status = Action {
    Ok("Extended status has not been implemented.")
  }

}
