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
import { FormGroup, FormControl, Popover, ControlLabel, Col, InputGroup, OverlayTrigger } from 'react-bootstrap'

export default class MusitTextField extends Component {

  static helpText(tip) {
    return (
      <Popover id="InputLinkPopover" title="Info">
        {tip}
      </Popover>
    )
  }

  render() {
    const type = (function(global) {
      var cache = {};
      return function(obj) {
        var key;
        return obj === null ? 'null' // null
          : obj === global ? 'global' // window in browser or global in nodejs
          : (key = typeof obj) !== 'object' ? key // basic: string, boolean, number, undefined, function
          : obj.nodeType ? 'object' // DOM element
          : cache[key = ({}).toString.call(obj)] // cached. date, regexp, error, object, array, math
        || (cache[key] = key.slice(8, -1).toLowerCase()); // get XXXX from [object XXXX], and cache it
      };
    }(this));

    return (
      <FormGroup
        controlId={this.props.controlId}
        style={{
          marginTop: 0,
          marginBottom: '5px'
        }}
        validationState={type(this.props.validationState) === 'function' ? this.props.validationState() : this.props.validationState}
      >
        <Col componentClass={ControlLabel} sm={2}>
          {this.props.labelText}
        </Col>
        <Col sm={10}>
          <InputGroup>
            <FormControl
              type={this.props.valueType}
              placeholder={this.props.placeHolderText}
              value={type(this.props.valueText) === 'function' ? this.props.valueText() : this.props.valueText}
              onChange={(event) => this.props.onChange(event.target.value)}
            />
            {this.props.tooltip ?
              <OverlayTrigger
                trigger={['click']}
                rootClose
                placement="right"
                overlay={MusitTextField.helpText(this.props.tooltip)}
              >
                <InputGroup.Addon>?</InputGroup.Addon>
              </OverlayTrigger> : null}
          </InputGroup>
        </Col>
      </FormGroup>
    )
  }
}

MusitTextField.propTypes = {
  controlId: PropTypes.string.isRequired,
  labelText: PropTypes.string.isRequired,
  placeHolderText: PropTypes.string.isRequired,
  tooltip: PropTypes.string,
  valueText: PropTypes.any,
  valueType: PropTypes.string,
  validationState: PropTypes.any,
  onChange: PropTypes.func.isRequired,
};
