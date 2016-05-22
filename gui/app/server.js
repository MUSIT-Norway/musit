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

import Express from 'express'
import React from 'react'
import ReactDOM from 'react-dom/server'
import config from './config'
import favicon from 'serve-favicon'
import compression from 'compression'
import httpProxy from 'http-proxy'
import path from 'path'
import createStore from './store/configureStore'
import ApiClient from './helpers/client-api'
import Html from './helpers/html'
import PrettyError from 'pretty-error'
import http from 'http'
import { match } from 'react-router'
import { ReduxAsyncConnect, loadOnServer } from 'redux-async-connect'
import createHistory from 'react-router/lib/createMemoryHistory'
import { syncHistoryWithStore } from 'react-router-redux'
import { Provider } from 'react-redux'
import getRoutes from './routes'
import Passport from 'passport'
import { Strategy as DataportenStrategy } from 'passport-dataporten'
import { Strategy as BearerStrategy } from 'passport-http-bearer'
import request from 'superagent'
const targetUrl = `http://${config.apiHost}:${config.apiPort}`
const pretty = new PrettyError()
const app = new Express()
const server = new http.Server(app)
const proxy = httpProxy.createProxyServer({
  target: targetUrl,
  ws: true
})

app.use(compression())
app.use(favicon(path.join(__dirname, '..', 'static', 'favicons', 'unimusfavicon.ico')))

app.use(Express.static(path.join(__dirname, '..', 'static')))

/* *** START Security *** */
const dataportenCallbackUrl = `http://${config.host}:8080/musit`
let passportStrategy = null
let passportLoginType = null
console.log(dataportenCallbackUrl)

if (config.FAKE_STRATEGY === config.dataportenClientSecret) {
  console.log(' Installing strategy: LOCAL Bearer')
  passportLoginType = 'bearer'

  const findToken = (token) => {
    const securityDatabase = require('./fake_security.json')
    return securityDatabase.users.find((user) => user.accessToken === token)
  }

  const localCallback = (token, done) => {
    const user = findToken(token)
    if (!user) {
      done(null, false, { message: 'Incorrect token.' })
    } else {
      done(null, user)
    }
  }

  passportStrategy = new BearerStrategy(localCallback)
} else {
  // TODO: Consider placing this initialization strategy into the config object
  console.log(' Installing strategy: DATAPORTEN')
  passportLoginType = 'dataporten'
  const dpConfig = {
    clientID: config.dataportenClientID,
    clientSecret: config.dataportenClientSecret,
    callbackURL: dataportenCallbackUrl
  }

  const dpCallback = (accessToken, refreshToken, profile, done) => {
    // load user and return done with the user in it.

    // TODO: Add user info to redux state
    return done(null, {
      userId: profile.data.id,
      name: profile.data.displayName,
      emails: profile.data.emails,
      photos: profile.data.photos,
      accessToken: accessToken
    })
  }

  passportStrategy = new DataportenStrategy(dpConfig, dpCallback)
}

Passport.serializeUser((user, done) => {
  done(null, user);
})

Passport.deserializeUser((user, done) => {
  done(null, user);
})

Passport.use(
  passportStrategy
)

app.use(Passport.initialize())

// Proxy to API server
app.use('/api', (req, res) => {
  proxy.web(req, res, { target: targetUrl })
})

app.use('/ws', (req, res) => {
  proxy.web(req, res, { target: `${targetUrl}/ws` })
})

server.on('upgrade', (req, socket, head) => {
  proxy.ws(req, socket, head)
})

// added the error handling to avoid https://github.com/nodejitsu/node-http-proxy/issues/527
proxy.on('error', (error, req, res) => {
  if (error.code !== 'ECONNRESET') {
    console.error('proxy error', error)
  }

  if (!res.headersSent) {
    res.writeHead(500, { 'content-type': 'application/json' })
  }

  res.end(JSON.stringify({ error: 'proxy_error', reason: error.message }))
})

const renderApplication = (req, res, store, status, component) => {
  global.navigator = { userAgent: req.headers['user-agent'] };
  const html = ReactDOM.renderToString(<Html assets={webpackIsomorphicTools.assets()} component={component} store={store} />)
  res.status(status).send(`<!doctype html>\n${html}`)
};

app.get('/', (req, res) => {
  const store = createStore(new ApiClient(req));
  renderApplication(req, res, store, 200);
});

app.use('/musit', Passport.authenticate(passportLoginType, { failWithError: true }),
  (req, res) => {
    if (__DEVELOPMENT__) {
      // Do not cache webpack stats: the script file would change since
      // hot module replacement is enabled in the development env
      webpackIsomorphicTools.refresh()
    }
    const client = new ApiClient(req)
    const virtualBrowserHistory = createHistory(req.originalUrl)

    request.get(`${targetUrl}/api/core/v1/my-groups`).accept('application/json')
      .set('Authorization', `Bearer ${req.user.accessToken}`).end((saErr, saRes) => {
        if (saErr) {
          res.status(401).send(saRes.body)
        } else {
          const store = createStore(client, {
            auth: {
              user: req.user,
              groups: saRes.body
            }
          })

          const history = syncHistoryWithStore(virtualBrowserHistory, store)

          if (__DISABLE_SSR__) {
            renderApplication(req, res, store, 200);
          } else {
            match({
              history,
              routes: getRoutes(store),
              location: req.originalUrl
            }, (error, redirectLocation, renderProps) => {
              if (redirectLocation) {
                res.redirect(redirectLocation.pathname + redirectLocation.search)
              } else if (error) {
                console.error('ROUTER ERROR:', pretty.render(error));
                renderApplication(req, res, store, 500);
              } else if (renderProps) {
                loadOnServer({ ...renderProps, store, helpers: { client } }).then(() => {
                  const component = (
                  <Provider store={store} key="provider">
                    <ReduxAsyncConnect {...renderProps} />
                  </Provider>
                );
                  renderApplication(req, res, store, 200, component);
                })
              } else {
                res.status(404).send('Not found')
              }
            })
          }
        }
      })
  },
  (err, req, res) => {
    res.redirect('/')
  }
)

if (config.port) {
  server.listen(config.port, (err) => {
    if (err) {
      console.error(err)
    }
    console.info('----\n==> ✅  %s is running, talking to API server on %s.', config.app.title, config.apiPort)
    console.info('==> 💻  Open http://%s:%s in a browser to view the app.', config.host, config.port)
  })
} else {
  console.error('==>     ERROR: No PORT environment variable has been specified')
}
