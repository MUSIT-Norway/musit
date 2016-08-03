/**
 * Created by steinaol on 5/31/16.
 */

import React, { Component, PropTypes } from 'react';
import Select from 'react-select'
import validate from '../common/validators'

export default class MusitDropDownField extends Component {

  classNameOnlyWithInput() {
    let lvString = ''
    if (validate(this.props) === 'error') {
      lvString = 'has-error'
    } else {
      lvString = ''
    }
    return lvString
  }

  classNameWithSpan() {
    let lvString = ' '
    if (validate(this.props) === 'error') {
      lvString = 'input-group has-error'
    } else {
      lvString = 'input-group'
    }
    return lvString
  }


  render() {
    const v = this.props.value ? this.props.value : '';
    const options = this.props.items.map((el) => (
      {
        value: el,
        label: this.props.translateKeyPrefix ? this.props.translate(this.props.translateKeyPrefix.concat(el)) : el
      }
    ));
    const lcAddOnPrefix = this.props.addOnPrefix ? <span className="input-group-addon" >{this.props.addOnPrefix}</span> : null;
    const lcPlaceholder = (
      <Select
        id={this.props.id}
        disabled={this.props.disabled}
        name="form-field-name"
        value={v}
        options={options}
        onChange={(event) => this.props.onChange(event.value)}
        data-toggle="tooltip"
        title={this.props.tooltip}
        clearable={false}
      />);
    const lcHelp = this.props.help ? <span className="input-group-addon" >?</span> : null;

    return (lcAddOnPrefix !== null || lcHelp !== null) ? (
      <div
        className={this.classNameWithSpan()}
      >
        {lcAddOnPrefix}
        {lcPlaceholder}
        {lcHelp}
      </div>
     ) : (
      <div
        className={this.classNameOnlyWithInput()}
      >
          {lcPlaceholder}
      </div>
      )
  }
}


MusitDropDownField.propTypes = {
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
  validator: PropTypes.string,
  precision: PropTypes.number,
  items: PropTypes.array.isRequired,
  translate: PropTypes.func,
  translateKeyPrefix: PropTypes.string,
  disabled: PropTypes.bool
};
