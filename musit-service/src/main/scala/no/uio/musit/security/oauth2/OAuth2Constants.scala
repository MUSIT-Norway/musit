/*
 * MUSIT is a museum database to archive natural and cultural history data.
 * Copyright (C) 2016  MUSIT Norway, part of www.uio.no (University of Oslo)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package no.uio.musit.security.oauth2

trait OAuth2Constants {

  val ClientID          = "client_id"
  val ClientSecret      = "client_secret"
  val RedirectURI       = "redirect_uri"
  val Scope             = "scope"
  val ResponseType      = "response_type"
  val State             = "state"
  val GrantType         = "grant_type"
  val AuthorizationCode = "authorization_code"
  val AccessToken       = "access_token"
  val Error             = "error"
  val Code              = "code"
  val TokenType         = "token_type"
  val ExpiresIn         = "expires_in"
  val Expires           = "expires"
  val RefreshToken      = "refresh_token"
  val AccessDenied      = "access_denied"

}
