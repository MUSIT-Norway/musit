#
#  MUSIT is a museum database to archive natural and cultural history data.
#  Copyright (C) 2016  MUSIT Norway, part of www.uio.no (University of Oslo)
#
#  This program is free software; you can redistribute it and/or modify
#  it under the terms of the GNU General Public License as published by
#  the Free Software Foundation; either version 2 of the License,
#  or any later version.
#
#  This program is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#  GNU General Public License for more details.
#
#  You should have received a copy of the GNU General Public License along
#  with this program; if not, write to the Free Software Foundation, Inc.,
#  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Example schema
 
# --- !Ups

--CREATE SEQUENCE IF NOT EXISTS GLOBAL_SEQ START WITH 1 INCREMENT BY 1 NOCACHE;

--CREATE TABLE URI_LINKS (
  -- ID bigint DEFAULT GLOBAL_SEQ.nextval NOT NULL,
   --LOCAL_TABLE_ID bigint NOT NULL,
   --REL varchar(255) NOT NULL,
   --HREF varchar(2000) NOT NULL,
   --PRIMARY KEY (ID)
--);
 
# --- !Downs
 
--DROP TABLE URI_LINKS;

--DROP SEQUENCE GLOBAL_SEQ;