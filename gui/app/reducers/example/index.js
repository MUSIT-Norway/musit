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
 
import * as types from '../../constants/example'
import * as actions from '../../actions/example'
import expect from 'expect'
import deepFreeze from 'deep-freeze'

const defaultState = {
	state1: 'default_state1',
	state2: 'default_state2'
}
deepFreeze(defaultState)

const exampleReducer = (state = defaultState, action) => {
	switch (action.action) {
		case types.UPDATE_STATE1_MESSAGE:
		    return Object.assign({}, state, {state1: action.text})
		case types.UPDATE_STATE2_MESSAGE:
			return Object.assign({}, state, {state2: action.text})
		default:
		    return state
	}
}

export default exampleReducer

// Tests
expect(exampleReducer(defaultState, {}).state1).toEqual('default_state1')
expect(exampleReducer(undefined, {}).state1).toEqual('default_state1')
expect(exampleReducer(defaultState, actions.updateState1('test2')).state1).toEqual('test2')

console.log('Example reduser tests: PASSED')