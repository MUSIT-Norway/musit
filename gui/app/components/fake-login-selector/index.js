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

import React, { Component, PropTypes } from 'react'
import { connect } from 'react-redux'
import { DropdownButton, MenuItem } from 'react-bootstrap'
import config from '../../config'


const mapStateToProps = (state) => {
  return {
    fakeAuthInfo: state.fakeAuthInfo
  }
}

@connect(mapStateToProps)
export default class FakeLoginSelector extends Component {
  static propTypes = {
    fakeAuthInfo: PropTypes.object
  }

  static contextTypes = {
    store: PropTypes.object.isRequired
  }

  onSelect(token) {
    console.log(`Trying to fake-login ${token}`)
    window.location.href = `http://${config.host}:${config.port}/musit?access_token=${token}`
  }

  render() {
    const { fakeAuthInfo } = this.props

    let menuItems = ''
    if (fakeAuthInfo.loaded) {
      menuItems = fakeAuthInfo.users.map((user) => <MenuItem eventKey={user.accessToken}>{user.name}</MenuItem>)
    }

    return (
      <DropdownButton title="Fake User" onSelect={this.onSelect}>
        {menuItems}
      </DropdownButton>
    )
  }
}
