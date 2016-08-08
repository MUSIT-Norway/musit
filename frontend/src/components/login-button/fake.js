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
import { connectUser } from '../../reducers/auth'

const mapStateToProps = (state) => {
  return {
    users: state.fakeAuthInfo.users || []
  }
}

const mapDispatchToProps = (dispatch) => {
  return {
    setUser: (user) => dispatch(connectUser(user))
  }
}

@connect(mapStateToProps, mapDispatchToProps)
export default class FakeLoginSelector extends Component {
  static propTypes = {
    users: PropTypes.arrayOf(PropTypes.object),
    setUser: PropTypes.func.isRequired,
  }

  static contextTypes = {
    store: PropTypes.object.isRequired
  }

  constructor(props) {
    super(props);
    this.onSelect = this.onSelect.bind(this);
  }

  doLogin(user) {
    this.props.setUser(user);
  }

  render() {
    return (
      <DropdownButton title="Fake User" onSelect={this.doLogin}>
        {this.props.users.map((user) => <MenuItem eventKey={user}>{user.name}</MenuItem>)}
      </DropdownButton>
    )
  }
}
