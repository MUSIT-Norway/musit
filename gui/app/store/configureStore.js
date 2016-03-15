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

import { applyMiddleware, createStore, compose, combineReducers } from 'redux'
import BrowserWebsocket from 'browser-websocket'
import createWSMiddleware from '../middleware/createWSMiddleware'
import thunkMiddleware from 'redux-thunk'
import createLogger from 'redux-logger'
import DevTools from '../components/dev-tools'
import rootReducer from '../reducers'

let aripCreateStore = null

try {
  //const sock = new BrowserWebsocket('ws://localhost:8080/v1/event/test', true)
  //const websocketMiddleware = createWSMiddleware(sock)

  aripCreateStore = compose(
    applyMiddleware(thunkMiddleware),
    //applyMiddleware(websocketMiddleware),
    applyMiddleware(createLogger()),
    DevTools.instrument()
  )(createStore)
} catch (e) {
  aripCreateStore = compose(
    applyMiddleware(thunkMiddleware),
    applyMiddleware(createLogger()),
    DevTools.instrument()
  )(createStore)
}

export default function configureStore(initialState) {
	const store = aripCreateStore(rootReducer, initialState)
    if (module.hot) {
        // Enable Webpack hot module replacement for reducers
        module.hot.accept('../reducers', () => {
            const nextRootReducer = require('../reducers')
            store.replaceReducer(nextRootReducer)
        })
    }
    console.log(store)
    return store
}
