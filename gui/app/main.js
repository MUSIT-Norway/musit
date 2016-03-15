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
 
import React from 'react'
import ReactDOM from 'react-dom'
import { Provider } from 'react-redux'
import { Router, Route, IndexRoute } from 'react-router'
import { syncReduxAndRouter } from 'redux-simple-router'
import { createHashHistory as createHistory } from 'history'
import configureStore from './store/configureStore.js'
import MainIndex from './components/main_index'
import WelcomeView from './containers/welcome-view'
import ExampleView from './containers/example-view'

const store = configureStore()
const history = createHistory()

syncReduxAndRouter(history, store)

const rootElement = document.getElementById('root')

ReactDOM.render(
	<Provider store={store}>
	  <div>
	  <Router history={history}>
	    <Route path="/" component={MainIndex}>
	      <IndexRoute component={WelcomeView} />
          <Route path="example" component={ExampleView} />
        </Route>
	  </Router>
	  </div>
    </Provider>,
    rootElement
)
