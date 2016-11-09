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

require('babel-polyfill');

export default {
  host: process.env.HOST || 'localhost',
  port: process.env.PORT || 8080,
  gatewayHost: process.env.APIHOST || 'localhost',
  gatewayPort: process.env.APIPORT || 3030,
  dataportenClientID: process.env.CLIENT_ID || 'ccee5f45-6f32-4315-9a89-9e6ad98a8186',
  dataportenClientSecret: process.env.CLIENT_SECRET || 'd01c882a-b24c-4ebf-9035-381a9a8cd74e',
  dataportenCallbackUrl: process.env.CALLBACK_URL || 'http://musit-test:8888/musit',
  jwtSecret: process.env.JWT_SECRET || 'K456thizI33ReallySecr#t127'
};
