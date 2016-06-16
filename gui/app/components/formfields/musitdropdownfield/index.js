/**
 * Created by steinaol on 5/31/16.
 */

import React, { Component, PropTypes } from 'react';
import { FormGroup, Popover, ControlLabel, Col, InputGroup, OverlayTrigger, FormControl } from 'react-bootstrap'


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
                    <FormControl
                      type = {this.props.valueType}
                      componentClass="select"
                      style={{ borderRadius: '5px' }}
                      onChange={(event) => this.props.onChange(event.target.value) }

                    >
                      <option>Velg type</option>
                      {this.props.items.map((el) =>
                          <option value = {el}> {el} </option>)
                      }
                    </FormControl>

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
