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
import { connect } from 'react-redux'


const mapStateToProps = (state) => ({
  suggest: state.suggest,
  user: state.auth.user,
  environmentRequirements: state.environmentRequirements
})

const mapDispatchToProps = (dispatch) => ({
  dispatchCreateControl: (control) => {
    if (control && control.date && control.date.length > 0) {
      dispatch() // Todo: Create reducer etc
    }
  }
})

@connect(mapStateToProps, mapDispatchToProps)

export default class ControlView extends React.Component {
  static propTypes = {
    translate: React.PropTypes.func.isRequired,
    user: React.PropTypes.object.isRequired,
    date: React.PropTypes.string.isRequired,
    unit: React.PropTypes.shape({
      temperatureOK: React.PropTypes.bool.isRequired,
      inertAirOK: React.PropTypes.bool.isRequired,
      gasOK: React.PropTypes.bool.isRequired,
      lightConditionsOK: React.PropTypes.bool.isRequired,
      cleaningOK: React.PropTypes.bool.isRequired,
      alchoholOK: React.PropTypes.bool.isRequired,
      moldFungusOK: React.PropTypes.bool.isRequired,
      pestOK: React.PropTypes.bool.isRequired,
      storageUnit: React.PropTypes.bool.isRequired
    })
  }

  constructor(props) {
    super(props)

    this.state = {
      date: '',
      user: '',
      temperatureOK: '',
      inertAirOK: '',
      gasOK: '',
      lightConditionsOK: '',
      cleaningOK: '',
      alchoholOK: '',
      moldFungusOK: '',
      pestOK: '',
      storageUnit: ''
    }
  }
}
