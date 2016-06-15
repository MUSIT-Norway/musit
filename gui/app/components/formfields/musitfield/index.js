
import React, { Component, PropTypes } from 'react';
import { Popover } from 'react-bootstrap'
import { validate } from '../common/validators'

export default class MusitField extends Component {

  static helpText(tip) {
    return (
      <Popover id="InputLinkPopover" title="Info">
        {tip}
      </Popover>
    )
  }

  classNameWithSpan() {
    return validate(this, this.props.value, this.props.validate,
      { minimumLength: this.props.minimumLength, maximumLength: this.props.maximumLength }) === 'error'
      ? 'input-group has-error' : 'input-group'
  }

  classNameOnlyWithInput() {
    return validate(this, this.props.value, this.props.validate,
      this.props.minimumLength, { maximumLength: this.props.maximumLength }) === 'error' ? 'has-error' : '';
  }


  render() {
    const lcAddOnPrefix = this.props.addOnPrefix ? <span className="input-group-addon" >{this.props.addOnPrefix}</span> : null;
    const lcPlaceholder = (
      <input type="text" className="form-control"
        placeholder={ this.props.placeHolder } value={ this.props.value } id={ this.props.id }
        onChange={(event) => this.props.onChange(event.target.value)}
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
