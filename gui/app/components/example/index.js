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

import React, { Component, PropTypes, bindActionCreators } from 'react';
import { connect } from 'react-redux';
import * as actions from '../../actions/example';
import Button from 'react-bootstrap/lib/Button';

const mapStateToProps = (state) => {
  return {
    example: state.example,
  };
};

@connect(mapStateToProps)
export default class Example extends Component {

  static propTypes = {
    example: PropTypes.object,
    dispatch: PropTypes.func.isRequired,
  };

  static contextTypes = {
    store: PropTypes.object.isRequired,
  };

  constructor(props) {
    super(props);
    const { dispatch, example } = this.props;
    this.onState1Click = this.onState1Click.bind(example, dispatch);
    this.onState2Click = this.onState2Click.bind(example, dispatch);
  }

  onState1Click(dispatch) {
    dispatch(actions.updateState1(this.state1 + 'a'));
  }

  onState2Click(dispatch) {
    dispatch(actions.updateState2(this.state2 + 'b'));
  }

  render() {
    return (
        <div>
            <Text>State ({this.props.example.state1}, {this.props.example.state2})</Text>
            <Button
              bsSize="xsmall"
              onClick={this.onState1Click}
            >
              1+
            </Button>
            <Button
              bsSize="xsmall"
              onClick={this.onState2Click}
            >
              2+
            </Button>
        </div>
    );
  }
}
