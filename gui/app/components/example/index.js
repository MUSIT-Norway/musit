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
 
import React, { Component, PropTypes, bindActionCreators } from 'react'
import { connect } from 'react-redux'
import * as actions from '../../actions/example'
import Button from 'react-bootstrap/lib/Button'

const mapStateToProps = (state) => {
    return {
        example: state.example
    }
}

@connect(mapStateToProps)
export default class Example extends Component {
    static propTypes = {
      example: PropTypes.object
    }

    static contextTypes = {
      store: PropTypes.object.isRequired
    }

    onState1Click(dispatch) {
        dispatch(actions.updateState1(this.state1 + 'a'))
    }

    onState2Click(dispatch) {
        dispatch(actions.updateState2(this.state2 + 'b'))
    }

	render () {
		const { dispatch, example, onState1Click, onState2Click } = this.props

		return (
			<div>
				State ({example.state1}, {example.state2}) <Button bsSize="xsmall" onClick={this.onState1Click.bind(example, dispatch)}>1+</Button><Button bsSize="xsmall" onClick={this.onState2Click.bind(example, dispatch)}>2+</Button>
			</div>
    	)
	}
}
