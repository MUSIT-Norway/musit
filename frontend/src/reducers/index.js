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

import { routerReducer } from 'react-router-redux'
import { reducer as form } from 'redux-form'
import { combineReducers } from 'redux'

import infoReducer from './info'
import authReducer from './auth'
import fakeAuthReducer from './fake-auth-info'
import languageReducer from './language'
import autosuggestReducer from './suggest'
import picklistReducer from './picklist'
import storagePanelReducer from './storageunit/panel'
import storageGridReducer from './storageunit/grid'
import organizationReducer from './organization'

const rootReducer = combineReducers({
  routing: routerReducer,
  info: infoReducer,
  auth: authReducer,
  fakeAuthInfo: fakeAuthReducer,
  language: languageReducer,
  suggest: autosuggestReducer,
  picks: picklistReducer,
  storagePanelUnit: storagePanelReducer,
  storageGridUnit: storageGridReducer,
  organization: organizationReducer,
  form: form
})

export default rootReducer
