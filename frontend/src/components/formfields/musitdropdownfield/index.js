/**
 * Created by steinaol on 5/31/16.
 */

import React, { Component, PropTypes } from 'react';
import { FormGroup, Popover, ControlLabel, Col, InputGroup, OverlayTrigger } from 'react-bootstrap'
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
    const v = this.props.valueText() ? this.props.valueText() : '';
    const options = [{ value: '', label: 'Velg type' }]
      .concat(
        this.props.items.map((el) =>
        ({ value: el, label: this.props.translate('musit.storageUnits.storageType.items.'.concat(el)) }))
      );

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
            <Select
              name="form-field-name"
              value={v}
              options={options}
              onChange={(event) => this.props.onChange(event.value)}
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
  translate: PropTypes.func.isRequired
};
