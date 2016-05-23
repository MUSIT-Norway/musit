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
import { IndexRedirect, IndexRoute, Route } from 'react-router'
import NotFound from './components/NotFound'
import WelcomeView from './containers/welcome-view'
import StorageUnitsView from './containers/storageunits-view'
import WelcomeUserView from './containers/welcome-user'
import App from './containers/app'

export default (store) => {
  const requireLogin = (nextState, replace, cb) => {
    const { auth: { user } } = store.getState();
    if (!user) {
      replace('/');
    }
    cb();
  };

  const redirectIfLoggedIn = (nextState, replace, cb) => {
    const { auth: { user } } = store.getState();
    if (user) {
      replace('/musit');
    }
    cb();
  };

  /**
   * Please keep routes in alphabetical order
   */
  return (
    <Route component={App}>
        <IndexRedirect to="/" />

        <Route path="/" component={WelcomeView} onEnter={redirectIfLoggedIn} />

       <Route path="/storageunits" component={StorageUnitsView} />

        -- Authentication routes
        <Route path="/musit" onEnter={requireLogin}>
          <IndexRoute component={WelcomeUserView} />
        </Route>

        -- Catch all route
        <Route path="/*" component={NotFound} status={404} />
    </Route>
  );
};
