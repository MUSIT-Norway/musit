/**
 * Created by steinaol on 5/31/16.
 */

import React, { Component, PropTypes } from 'react';
import { FormGroup, Popover, ControlLabel, Col, InputGroup, OverlayTrigger, DropdownButton, MenuItem } from 'react-bootstrap'


export default class MusitDropDownField extends Component {

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
                <InputGroup style={{ display: 'table' }}>
                <DropdownButton title={this.props.valueText()} id="bg-nested-dropdown">
                    <MenuItem eventKey="1">Velg type</MenuItem>
                    {this.props.items.map((el) => {
                      if (el === this.props.valueText()) {
                        return <MenuItem eventKey="2" active>{el}</MenuItem>
                      }
                      return <MenuItem eventKey="2">{el}</MenuItem>
                    }
                  )
                }
                </DropdownButton>
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
