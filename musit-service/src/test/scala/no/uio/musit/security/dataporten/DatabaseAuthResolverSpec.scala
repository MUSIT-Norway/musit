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

package no.uio.musit.security.dataporten

import java.util.UUID

import no.uio.musit.models.{ActorId, Email}
import no.uio.musit.security.{BearerToken, SessionUUID, UserInfo, UserSession}
import no.uio.musit.test.MusitSpecWithAppPerSuite
import org.joda.time.DateTime
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.ExecutionContext.Implicits.global

class DatabaseAuthResolverSpec extends MusitSpecWithAppPerSuite {

  // Wire up the dependencies
  val config = fromInstanceCache[DatabaseConfigProvider]
  val resolver = new DatabaseAuthResolver(config)

  val uuid = ActorId.generate()
  val email = "dv@deathstar.io"

  val sessionUUID = SessionUUID.generate()

  val userInfo = UserInfo(
    id = uuid,
    secondaryIds = Option(Seq(email)),
    name = Option("Darth Vader"),
    email = Option(Email("darth.vader@deathstar.io")),
    picture = None
  )

  "The DatabaseAuthResolver" when {

    "working with UserInfo data" should {

      "insert new UserInfo" in {
        resolver.saveUserInfo(userInfo).futureValue.isSuccess mustBe true
      }

      "find a UserInfo by " in {
        val res = resolver.userInfo(uuid).futureValue
        res.isSuccess mustBe true
        res.get mustBe Some(userInfo)
      }

      "convert email fields to lower case when storing UserInfo" in {
        val ui = UserInfo(
          id = ActorId.generate(),
          secondaryIds = Option(Seq(email.toUpperCase)),
          name = Option("Darth Vader"),
          email = Option(Email("darth.vader@deathstar.io")),
          picture = None
        )

        resolver.saveUserInfo(ui).futureValue.isSuccess mustBe true

        val res = resolver.userInfo(ui.id).futureValue
        res.isSuccess mustBe true
        res.get.isDefined mustBe true
        res.get.get.secondaryIds must not be empty
        res.get.get.secondaryIds.get.head mustBe email.toLowerCase
      }

      "update existing UserInfo" in {
        val upd = userInfo.copy(name = Some("Darth Anakin Vader"))
        resolver.saveUserInfo(upd).futureValue.isSuccess mustBe true
        val res = resolver.userInfo(uuid).futureValue
        res.isSuccess mustBe true
        res.get mustBe Some(upd)
      }
    }

    "when working with UserSession data" should {

      "initialize an empty UserSession and return the SessionUUID" in {
        val res = resolver.upsertUserSession(UserSession(uuid = sessionUUID)).futureValue
        res.isSuccess mustBe true
      }

      "find and update an UserSession" in {
        val sessionRes = resolver.userSession(sessionUUID).futureValue
        sessionRes.isSuccess mustBe true
        sessionRes.get must not be None
        val session = sessionRes.get.get
        session.uuid mustBe sessionUUID

        val currDateTime = DateTime.now

        val upd = session.copy(
          oauthToken = Option(BearerToken(UUID.randomUUID().toString)),
          userId = ActorId.generateAsOpt(),
          loginTime = Some(currDateTime),
          lastActive = Some(currDateTime),
          isLoggedIn = true,
          tokenExpiry = Some(DateTime.now.plusHours(4).getMillis)
        )

        resolver.updateSession(upd).futureValue.isSuccess mustBe true

        val res = resolver.userSession(sessionUUID).futureValue
        res.isSuccess mustBe true
        res.get mustBe Some(upd)
      }

    }

  }

}
