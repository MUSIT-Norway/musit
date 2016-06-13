
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
    const p = this.props.addOnPrefix ? <span className="input-group-addon" >{this.props.addOnPrefix}</span> : null;
    const i = <input type="text" className="form-control" placeholder={ this.props.placeHolder } id={ this.props.id } />;
    const s = this.props.help ? <span className="input-group-addon" >?</span> : null;
    return (p !== null || s !== null) ? (
        <div className="input-group">
          {p}
          {i}
          {s}
        </div>
     ) : i
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
