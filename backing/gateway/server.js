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
import express from 'express';
import config from '../api/config';
import APIGateway from './index';
const Log = require('log');
const logger = new Log('info');

const app = express();
app.locals.gateway = new APIGateway('/api');

app.use((req, res) => {
  if (app.locals.gateway.validCall(req.url)) {
    app.locals.gateway.call(req, res);
  } else {
    res.status(404).send('NOT FOUND');
  }
});

if (config.apiPort) {
  app.listen(config.apiPort, (err) => {
    if (err) {
      logger.error(err);
    }
    logger.info('----\n==> 🌎  Gateway is running on port %s', config.apiPort);
  });
} else {
  logger.error('==>     ERROR: No PORT environment variable has been specified');
}
