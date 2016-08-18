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

import { applyMiddleware, createStore as _createStore, compose } from 'redux'
import rootReducer from '../reducers'
import createMiddleware from '../middleware/clientMiddleware'
const createLogger = require('redux-logger')
const { persistState } = require('redux-devtools')
const DevTools = require('../components/dev-tools')
const reducers = require('../reducers')

const createStore = (client, data) => {
  const middleware = [createMiddleware(client)]

  let finalCreateStore;
  if (__DEVELOPMENT__) {
    const logger = createLogger()
    finalCreateStore = compose(
      applyMiddleware(logger, ...middleware),
      window.devToolsExtension ? window.devToolsExtension() : DevTools.instrument(),
      persistState(window.location.href.match(/[?&]debug_session=([^&]+)\b/))
    )(_createStore)
  } else {
    finalCreateStore = applyMiddleware(...middleware)(_createStore)
  }

  const store = finalCreateStore(rootReducer, data)

  // reduxRouterMiddleware.listenForReplays(store);

  if (__DEVELOPMENT__ && module.hot) {
    module.hot.accept('../reducers', () => {
      store.replaceReducer(reducers)
    })
  }

  return store
};

export default createStore;
