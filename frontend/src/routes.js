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
import { IndexRedirect, Route } from 'react-router'
import NotFound from './components/NotFound'
import WelcomeView from './containers/welcome-view'
import StorageUnitsTable from './containers/storageunit/grid'
import PickListView from './containers/picklist'
import ExampleView from './containers/example-view'
import StorageUnitPanel from './containers/storageunit/panel'
import WelcomeUserView from './containers/welcome-user'
import ObservationView from './containers/observation'
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
      replace('/musit/');
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

      <Route path="/picklist" component={PickListView} />
      <Route path="/magasin" component={StorageUnitsTable} />
      <Route path="/example" component={ExampleView} />
      <Route path="/observation" component={ObservationView} />
      <Route path="/storageunit/:id" component={StorageUnitPanel} />
      <Route path="/storageunit/add" component={StorageUnitPanel} />

      -- Authentication routes
      <Route path="/musit" component={WelcomeUserView} onEnter={requireLogin} />

      -- Catch all route
      <Route path="/*" component={NotFound} status={404} />
    </Route>
  );
};
