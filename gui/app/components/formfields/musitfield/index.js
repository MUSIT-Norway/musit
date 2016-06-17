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
import { validate } from '../common/validators'

export default class MusitField extends Component {

  classNameWithSpan() {
    let lvString = ' '
    if (validate(this, this.props.value, this.props.validate,
      { minimumLength: this.props.minimumLength, maximumLength: this.props.maximumLength }) === 'error') {
      lvString = 'input-group has-error'
    } else {
      lvString = 'input-group'
    }
    return lvString
  }

  classNameOnlyWithInput() {
    let lvString = ''
    if (validate(this, this.props.value, this.props.validate,
      this.props.minimumLength, { maximumLength: this.props.maximumLength }) === 'error') {
      lvString = 'has-error'
    } else {
      lvString = ''
    }
    return lvString
  }


  render() {
    const lcAddOnPrefix = this.props.addOnPrefix ? <span className="input-group-addon" >{this.props.addOnPrefix}</span> : null;
    const lcPlaceholder = (
      <input type="text" className="form-control"
        placeholder={ this.props.placeHolder } value={ this.props.value } id={ this.props.id }
        onChange={(event) => this.props.onChange(event.target.value)} data-toggle="tooltip" title={this.props.tooltip}
      />);
    const lcHelp = this.props.help ? <span className="input-group-addon" >?</span> : null;

    return (lcAddOnPrefix !== null || lcHelp !== null) ? (
        <div className= { this.classNameWithSpan() } >
          {lcAddOnPrefix}
          {lcPlaceholder}
          {lcHelp}
        </div>
     ) : (
        <div className= { this.classNameOnlyWithInput() } >
         { lcPlaceholder }
        </div>
      )
  }
}

MusitField.propTypes = {
  id: PropTypes.string.isRequired,
  value: PropTypes.string.isRequired, // Should be any
  addOnPrefix: PropTypes.string,
  help: PropTypes.string, // always ? on add on after
  placeHolder: PropTypes.string,
  tooltip: PropTypes.string,
  onChange: PropTypes.func.isRequired,
  validate: PropTypes.string.isRequired,
  minimumLength: PropTypes.number,
  maximumLength: PropTypes.number,
  validator: PropTypes.func
};
