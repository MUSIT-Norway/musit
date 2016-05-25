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

import React, { Component, PropTypes } from 'react';
import { FormGroup, ControlLabel, FormControl, Col } from 'react-bootstrap'

export default class MusitTextField extends Component {
  constructor(props) {
    super(props)
    this.handleChange = this.handleChange.bind(this)
  }

  handleChange(event) {
    this.props.onChange(event.target.value)
  }

  render() {
    return (
      <FormGroup
        controlId={this.props.controlId}
        style={{
          marginTop: 0,
          marginBottom: '5px'
        }}
      >
        <Col componentClass={ControlLabel} sm={2}>
          {this.props.labelText}
        </Col>
        <Col sm={5}>
          <FormControl
            type={this.props.valueType}
            placeholder={this.props.placeHolderText}
            value={this.props.valueText}
            onChange={this.handleChange}
          />
        </Col>
      </FormGroup>
    )
  }
}

MusitTextField.propTypes = {
  controlId: PropTypes.string.isRequired,
  labelText: PropTypes.string.isRequired,
  placeHolderText: PropTypes.string.isRequired,
  valueText: PropTypes.string,
  valueType: PropTypes.string,
  onChange: PropTypes.func.isRequired,
};
