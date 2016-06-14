
import React, { Component, PropTypes } from 'react';
import { Popover } from 'react-bootstrap';

export default class MusitField extends Component {

  static helpText(tip) {
    return (
      <Popover id="InputLinkPopover" title="Info">
        {tip}
      </Popover>
    )
  }

  render() {
    // <!-- Constant -->
    const LcAddOnPrefix = this.props.addOnPrefix ? <span className="input-group-addon" >{this.props.addOnPrefix}</span> : null;
    const LcPlaceholder = (
      <input type="text" className="form-control"
        placeholder={ this.props.placeHolder } value={ this.props.value } id={ this.props.id }
        onChange={(event) => this.props.onChange(event.target.value)}
      />);
    const LcHelp = this.props.help ? <span className="input-group-addon" >?</span> : null;
    return (LcAddOnPrefix !== null || LcHelp !== null) ? (
        <div className="input-group">
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
  validate: PropTypes.func.isRequired,
  onChange: PropTypes.func.isRequired,

};
