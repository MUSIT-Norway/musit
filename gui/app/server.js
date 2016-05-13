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
import {Provider} from 'react-redux'
import getRoutes from './routes'


const targetUrl = 'http://' + config.apiHost + ':' + config.apiPort
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

// Proxy to API server
app.use('/api', (req, res) => {
  proxy.web(req, res, {target: targetUrl})
})

app.use('/ws', (req, res) => {
  proxy.web(req, res, {target: targetUrl + '/ws'})
})

server.on('upgrade', (req, socket, head) => {
  proxy.ws(req, socket, head)
})

// added the error handling to avoid https://github.com/nodejitsu/node-http-proxy/issues/527
proxy.on('error', (error, req, res) => {
  let json
  if (error.code !== 'ECONNRESET') {
    console.error('proxy error', error)
  }
  if (!res.headersSent) {
    res.writeHead(500, {'content-type': 'application/json'})
  }

  json = {error: 'proxy_error', reason: error.message}
  res.end(JSON.stringify(json))
})

app.use((req, res) => {
  if (__DEVELOPMENT__) {
    // Do not cache webpack stats: the script file would change since
    // hot module replacement is enabled in the development env
    webpackIsomorphicTools.refresh()
  }
  const client = new ApiClient(req)
  const virtualBrowserHistory = createHistory(req.originalUrl)

  const store = createStore(client)

  const history = syncHistoryWithStore(virtualBrowserHistory, store)

  function hydrateOnClient() {
    res.send('<!doctype html>\n' +
      ReactDOM.renderToString(<Html assets={webpackIsomorphicTools.assets()} store={store}/>))
  }

  if (__DISABLE_SSR__) {
    hydrateOnClient()
    return
  }

  match({ history, routes: getRoutes(store), location: req.originalUrl }, (error, redirectLocation, renderProps) => {
    if (redirectLocation) {
      res.redirect(redirectLocation.pathname + redirectLocation.search)
    } else if (error) {
      console.error('ROUTER ERROR:', pretty.render(error))
      res.status(500)
      hydrateOnClient()
    } else if (renderProps) {
      loadOnServer({...renderProps, store, helpers: {client}}).then(() => {
        const component = (
          <Provider store={store} key="provider">
            <ReduxAsyncConnect {...renderProps} />
          </Provider>
        )

        res.status(200)

        global.navigator = {userAgent: req.headers['user-agent']}

        res.send('<!doctype html>\n' +
          ReactDOM.renderToString(<Html assets={webpackIsomorphicTools.assets()} component={component} store={store}/>))
      })
    } else {
      res.status(404).send('Not found')
    }
  })
})

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