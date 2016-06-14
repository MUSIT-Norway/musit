
import React, { Component, PropTypes } from 'react';
import { Popover } from 'react-bootstrap';

export default class MusitField extends Component {

  static validateString(value, minimumLength = 1, maximumLength = 20) {
    const isSomething = value.length >= minimumLength
    const isValid = isSomething ? 'success' : null
    return value.length > maximumLength ? 'error' : isValid
  }

  static validateNumber(value, minimumLength = 1) {
    const isSomething = value.length >= minimumLength
    const isValid = isSomething ? 'success' : null
    return isSomething && isNaN(value) ? 'error' : isValid
  }

  static validate(value, validateType = 'text') {
    let lValue = validateType;
    switch (validateType) {
      case 'text':
        lValue = MusitField.validateString(value);
        break;
      case 'number':
        lValue = MusitField.validateNumber(value);
        break;
      default:
        lValue = null;
    }
    return lValue;
  }

  static helpText(tip) {
    return (
      <Popover id="InputLinkPopover" title="Info">
        {tip}
      </Popover>
    )
  }

  lfClassName() {
    return MusitField.validate(this.props.value, this.props.validate) === 'error'
      ? 'input-group form-group has-error' : 'input-group';
  }


  render() {
    console.log(this.lfClassName());
    const LcAddOnPrefix = this.props.addOnPrefix ? <span className="input-group-addon" >{this.props.addOnPrefix}</span> : null;
    const LcPlaceholder = (
      <input type="text" className="form-control"
        placeholder={ this.props.placeHolder } value={ this.props.value } id={ this.props.id }
        onChange={(event) => this.props.onChange(event.target.value)}
      />);
    const LcHelp = this.props.help ? <span className="input-group-addon" >?</span> : null;
    return (LcAddOnPrefix !== null || LcHelp !== null) ? (
        <div className= { this.lfClassName() } >
          {LcAddOnPrefix}
          {LcPlaceholder}
          {LcHelp}
        </div>
     ) : LcPlaceholder
  }
}

MusitField.propTypes = {
  id: PropTypes.string.isRequired,
  value: PropTypes.string.isRequired,
  addOnPrefix: PropTypes.string,
  help: PropTypes.string, // always ? on add on after
  placeHolder: PropTypes.string,
  tooltip: PropTypes.string,
  onChange: PropTypes.func.isRequired,
  validate: PropTypes.string.isRequired,
  minimumLength: PropTypes.string.isRequired,
  maximumLength: PropTypes.string.isRequired,
};
