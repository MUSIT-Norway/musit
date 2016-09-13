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
import config from '../config';
import compression from 'compression';
import httpProxy from 'http-proxy';
import http from 'http';
import Passport from 'passport';
import { Strategy as DataportenStrategy } from 'passport-dataporten';
import jwt from 'jsonwebtoken';

const Log = require('log');
const logger = new Log('info');
const gatewayUrl = `http://${config.gatewayHost}:${config.gatewayPort}/api`;
const app = new Express();
const server = new http.Server(app);
const proxy = httpProxy.createProxyServer();
const jwtSecret = config.jwtSecret;

app.set('view engine', 'ejs');
app.use(compression());

/* *** START Security *** */
const passportStrategy = new DataportenStrategy({
  clientID: config.dataportenClientID,
  clientSecret: config.dataportenClientSecret,
  callbackURL: `http://${config.host}:8080/musit`,
}, (accessToken, refreshToken, profile, done) =>
  done(null, {
    userId: profile.data.id,
    name: profile.data.displayName,
    emails: profile.data.emails,
    photos: profile.data.photos,
    accessToken,
  }));
Passport.serializeUser((user, done) => done(null, user));
Passport.deserializeUser((user, done) => done(null, user));
Passport.use(passportStrategy);
app.use(Passport.initialize());
/* *** END Security *** */

// added the error handling to avoid https://github.com/nodejitsu/node-http-proxy/issues/527
proxy.on('error', (error, req, res) => {
  if (error.code !== 'ECONNRESET') {
    logger.error('proxy error', error);
  }
});

app.use('/musit', Passport.authenticate('dataporten', { failWithError: true }),
  (req, res) => {
    const token = jwt.sign({
      accessToken: req.user.accessToken,
      name: req.user.name,
      email: req.user.emails[0],
      userId: req.user.userId,
      avatarUrl: req.user.photos[0]
    }, jwtSecret);
    res.render('callback/index', { token });
  },
  (err, req, res) => {
    res.redirect('/?error=meh');
  }
);

// Proxy to API server
app.use('/api', (req, res) => {
  proxy.web(req, res, { target: gatewayUrl });
});

/* ----- DEVELOPMENT START ---- */
app.use('/assets', (req, res) => {
  proxy.web(req, res, { target: 'http://localhost:8000/assets' });
});
app.use('/*', (req, res) => {
  proxy.web(req, res, { target: 'http://localhost:8000' });
});
/* ----- DEVELOPMENT END ---- */

if (config.port) {
  server.listen(config.port, (err) => {
    if (err) {
      logger.error(err);
    }
    logger.info('----\n==> ✅  Frontend API is running on port %s, '
      + 'talking to Gateway server on %s.', config.port, config.gatewayPort);
  });
} else {
  logger.error('==>     ERROR: No PORT environment variable has been specified');
}
