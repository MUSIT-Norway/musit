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
import { IndexRoute, Route } from 'react-router'
import NotFound from './components/NotFound'
import WelcomeView from './containers/welcome-view'
import WelcomeUserView from './containers/welcome-user'
import App from './containers/app'

export default (store) => {
  const requireLogin = (nextState, replace, cb) => {
    function checkAuth() {
      const { auth: { user } } = store.getState()
      if (!user) {
        // oops, not logged in, so can't be here!
        replace('/')
      }
      cb()
    }

    checkAuth()
  };

  /**
   * Please keep routes in alphabetical order
   */
  return (
    <Route path="/musit" component={App}>
        <IndexRoute component={WelcomeView} />

        -- Authentication routes
        <Route onEnter={requireLogin}>
          <Route path="welcomeUser" component={WelcomeUserView} />
        </Route>

        -- Routes



        -- Catch all route
        <Route path="/musit/*" component={NotFound} status={404} />
    </Route>
  );
};
