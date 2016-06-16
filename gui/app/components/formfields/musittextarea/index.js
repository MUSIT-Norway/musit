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
import { FormGroup, FormControl, Popover, ControlLabel, Col, InputGroup, OverlayTrigger } from 'react-bootstrap';


export default class MusitTextArea extends Component {

  static helpText(tip) {
    return (
      <Popover id="InputLinkPopover" title="Info">
        {tip}
      </Popover>
    )
  }

  render() {
    return (
      <FormGroup
        controlId={this.props.controlId}
        style={{
          marginTop: 0,
          marginBottom: '5px'
        }}
        validationState={this.props.validationState()}
      >
        <Col componentClass={ControlLabel} sm={3}>
          {this.props.labelText}
        </Col>
        <Col sm={9}>
          <InputGroup>
              <FormControl
                componentClass="textarea"
                placeholder={this.props.placeHolderText}
                value={this.props.valueText()}
                onChange={(event) => this.props.onChange(event.target.value)}
              />

            {this.props.tooltip ?
              <OverlayTrigger
                trigger={['click']}
                rootClose
                placement="right"
                overlay={MusitTextArea.helpText(this.props.tooltip)}
              >
                <InputGroup.Addon>?</InputGroup.Addon>
              </OverlayTrigger> : null}
          </InputGroup>
        </Col>
      </FormGroup>
    )
  }
}

MusitTextArea.propTypes = {
  controlId: PropTypes.string.isRequired,
  labelText: PropTypes.string.isRequired,
  placeHolderText: PropTypes.string.isRequired,
  tooltip: PropTypes.string,
  valueText: PropTypes.func.isRequired,
  validationState: PropTypes.func.isRequired,
  onChange: PropTypes.func.isRequired,

};
