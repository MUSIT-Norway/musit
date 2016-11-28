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

package no.uio.musit.models

case class MuseumCollection() {

  /*
    We need to support the following (existing) collections by default.

    ---------------------------------------------------
    | English term               Norwegian term       | Seq(OLD_SCHEMA_NAME)
    ---------------------------------------------------
    | Archeology              |  Arkeologi            | (ARK_OSL, ARK_BRG)
    | Ethnography             |  Etnografi            |
    | Numismatics             |  Numismatikk          |
    | Lichen                  |  Lav                  |
    | Moss                    |  Mose                 |
    | Fungi                   |  Sopp                 |
    | Algae                   |  Alger                |
    | Vascular plants         |  Karplanter           |
    | Entomology              |  Entomologi           |
    | Marine invertebrates    |  Marine evertebrater  |
    |                         |                       |
    |                         |  ArkEtno              |  (ARK_OSL, ETNO_OSL)
    ---------------------------------------------------
  */

  /*
    TL;DR
    ----------------
    Museum collections should be stored in the MUSIT_AUTH schema in a separate
    table, AUTH_COLLECTION. There will then also need to be a table for
    USER_AUTH_COLLECTION to keep track of users with access to the collections.
    Much the same way as AUTH_GROUP <=> USER_AUTH_GROUP.


    Evaluation
    ----------------
    Ideally these should be configurable in some way or another. Quite possibly
    in a similar manner as how Auth Groups are handled. The option of keeping
    them in a configuration file is also somewhat appealing. Since it doesn't
    require the use of the DB.

    There are pros and cons to both solutions. Groups, and access to that DB,
    are already required by all services due to the authorisation implementation
    in the security lib. Having collection defs in the DB is also beneficial
    in that it provides the opportunity to add new collections at runtime. Which
    configuration files cannot do since we're living in Docker land. Also, it
    would require _all_ services to have the same config def at all time!

    Writing this has helped come to a conclusion! The more I think of it, the
    more it makes sense to use the DB. It requires only 1 point of data entry.
    Config would require the same amount of configs as there are running
    instances of services.

  */

}
