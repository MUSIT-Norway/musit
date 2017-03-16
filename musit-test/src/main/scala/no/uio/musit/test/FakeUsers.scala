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

package no.uio.musit.test

// scalastyle:off
object FakeUsers {

  val (fakeGuestId, fakeGuestToken) =
    ("1a17c60c-755d-4d88-9449-12bbd5abe5e2", "3136d121-3ec3-4ae1-893c-ed5755243e27")
  val (normalUserId, normalUserToken) =
    ("3655615b-e385-4833-b414-9957ea225e58", "5447f41c-b30b-44c4-8b3f-fda7e1dde949")
  val (superUserId, superUserToken) =
    ("896125d3-0563-46b6-a7c5-51f3f899ff0a", "94890490-f3b9-4233-a39d-c2613303f2d2")
  val (dbCoordId, dbCoordToken) =
    ("5aa63499-6491-4917-b577-3b161c75d489", "d8fb33c3-f2b4-4d2a-a24a-90fbfb9ecc5e")
  val (testUserId, testUserToken) =
    ("8efd41bb-bc58-4bbf-ac95-eea21ba9db81", "b1b1ae9a-b4cc-403f-924b-ca2b867f8732")
  val (testReadId, testReadToken) =
    ("caef058a-16d7-400f-9f73-3736477c6b44", "710c30ed-8777-4778-b3a3-873f7a8c5d81")
  val (testWriteId, testWriteToken) =
    ("a5d2a21b-ea1a-4123-bc37-6d31b81d1b2a", "239d84b2-07f7-4594-b42c-f2fc23cf55d3")
  val (testAdminId, testAdminToken) =
    ("d63ab290-2fab-42d2-9b57-2475dfbd0b3c", "5fb13ef2-4e81-4600-892b-6579cb17508c")
  val (nhmReadId, nhmReadToken) =
    ("ddb4dc62-8c14-4bac-aafd-0401f619b0ac", "54aa85c8-6212-4381-8d22-1d342cd8a26e")
  val (nhmWriteId, nhmWriteToken) =
    ("d2bad684-d41e-4b9f-b813-70a75ea1728a", "4f925018-2213-4319-b05a-518c1ef3d09b")
  val (nhmAdminId, nhmAdminToken) =
    ("84e47cd0-a9de-4513-9318-1dec4bac5433", "e36ead79-1d28-4adf-9e9c-23d003c1e6d4")
  val (vmReadId, vmReadToken) =
    ("690674c7-57ac-45d3-8f63-a764520ebe3f", "0652095b-58e4-4a01-b006-d71c0081365d")
  val (vmWriteId, vmWriteToken) =
    ("da673d83-342d-4f2c-b3bf-6136304e7131", "c66d676f-9762-455a-8182-a8ff2ada0e98")
  val (vmAdminId, vmAdminToken) =
    ("0967e7d7-a003-4fa5-88b2-55a0df0d8ed8", "c1127f6d-58e4-475f-a5e7-a200717c3016")

}
// scalastyle:on
