/*
 *  MUSIT is a museum database to archive natural and cultural history data.
 *  Copyright (C) 2016  MUSIT Norway, part of www.uio.no (University of Oslo)
 *
 *  This program is free software you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation either version 2 of the License,
 *  or any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
import Express from 'express';
import httpProxy from 'http-proxy';
import http from 'http';
const app = new Express();
const server = new http.Server(app);
const proxy = httpProxy.createProxyServer();

app.use('/musit', (req, res) => {
  proxy.web(req, res, { target: 'http://localhost:9090/musit' });
});

app.use('/api', (req, res) => {
  proxy.web(req, res, { target: 'http://localhost:9090/api' });
});

app.use('/', (req, res) => {
  proxy.web(req, res, { target: 'http://localhost:8000' });
});

server.listen(8080, (err) => {
  if (err) {
    console.error(err);
  } else {
    console.log('Started server');
  }
});
