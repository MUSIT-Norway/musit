import React, { Component, PropTypes } from 'react';
import FontAwesome from 'react-fontawesome';
import { Button, Row } from 'react-bootstrap';

export default class PairedToogleButtons extends Component {
  static propTypes = {
    label: PropTypes.string.isRequired,
    value: PropTypes.bool,
    updatevalueOK: PropTypes.func.isRequired,
    updatevalueNotOK: PropTypes.func.isRequired
  }

  render() {
    const { label, value } = this.props;
    const style = require('./index.scss')

    return (
      <div className={style.pageMargin}>
        <Row>
          {label}
        </Row>
        <Row>
          {value != null ? <FontAwesome name={value ? 'check-square-o' : 'times'} /> : <FontAwesome name="square-o" />}
          <Button
            className={value ? style.buttonpaddingtrue : style.buttonpaddingfalse}
            onClick={this.props.updatevalueOK}
          >
            <FontAwesome name="check-square-o" />
            <span>&nbsp;</span>
            OK
          </Button>
          <Button
            className={value != null && !value ? style.buttonpaddingtrue : style.buttonpaddingfalse}
            onClick={this.props.updatevalueNotOK}
          >
            <FontAwesome name="times" />
            <span>&nbsp;</span>
            IKKE ok
          </Button>
        </Row>
      </div>
    )
  }
}
