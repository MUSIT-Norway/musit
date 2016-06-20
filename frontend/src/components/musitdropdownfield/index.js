/**
 * Created by steinaol on 5/31/16.
 */

import React, { Component, PropTypes } from 'react';
import { FormGroup, Popover, ControlLabel, Col, InputGroup, OverlayTrigger, DropdownButton, MenuItem } from 'react-bootstrap'
import Select from 'react-select';

// Be sure to include styles at some point, probably during your bootstrapping
import 'react-select/dist/react-select.css';

export default class MusitDropDownField extends Component {

  static helpText(tip) {
    return (
      <Popover id="InputLinkPopover" title="Info">
          {tip}
      </Popover>
    )
  }

  render() {
    var options = [
      { value: 'one', label: 'One' },
      { value: 'two', label: 'Two' }
    ];

    function logChange(val) {
      console.log("Selected: " + val);
    }
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
          <InputGroup style={{ display: 'table' }}>
            <DropdownButton title={this.props.valueText()} id="bg-nested-dropdown">
              <MenuItem eventKey="1">Velg type</MenuItem>
              {this.props.items.map((el) => {
                if (el === this.props.valueText()) {
                  return <MenuItem eventKey="2" active>{el}</MenuItem>
                }
                return <MenuItem eventKey="2">{el}</MenuItem>
              })}
            </DropdownButton>
            <Select
              name="form-field-name"
              value="one"
              options={options}
              onChange={logChange}
            />
            {this.props.tooltip ?
              <OverlayTrigger
                trigger={['click']}
                rootClose
                placement="right"
                overlay={MusitDropDownField.helpText(this.props.tooltip)}
              >
                <InputGroup.Addon>?</InputGroup.Addon>
              </OverlayTrigger> : null}
          </InputGroup>
        </Col>
      </FormGroup>
    )
  }
}


MusitDropDownField.propTypes = {
  controlId: PropTypes.string.isRequired,
  labelText: PropTypes.string.isRequired,
  placeHolderText: PropTypes.string.isRequired,
  tooltip: PropTypes.string,
  items: PropTypes.array,
  valueText: PropTypes.func.isRequired,
  valueType: PropTypes.string.isRequired,
  validationState: PropTypes.func.isRequired,
  onChange: PropTypes.func,
};
