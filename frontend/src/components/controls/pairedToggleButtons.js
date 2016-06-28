import React, { Component, PropTypes } from 'react';
import FontAwesome from 'react-fontawesome';
import { Button, Row } from 'react-bootstrap';

export default class PairedToogleButtons extends Component {
  static propTypes = {
    label: PropTypes.string.isRequired,
    value: PropTypes.bool
  }

  render() {
    const { label, value } = this.props;
    const style = require('./index.scss')
    const styleset = value ? style.true : style.false
    const stackIcon = (icon1, icon2) => (
      <span className="fa-stack">
        <FontAwesome name={icon1} size="2x" stack="2x" />
        <FontAwesome name={icon2} size="1x" stack="1x" />
      </span>
    )

    return (
      <div>
        <Row>
          {label}
        </Row>
        <Row>
          {value != null ? <FontAwesome name={value ? 'check' : 'close'} /> : <span>&nbsp;&nbsp;&nbsp;</span>}
          <Button className={value ? styleset.buttonpaddingtrue : styleset.buttonpaddingfalse}>
            <FontAwesome name="check" />
            <span>&nbsp;</span>
            OK
          </Button>
          <Button className={value ? styleset.buttonpaddingtrue : styleset.buttonpaddingfalse}>
            <FontAwesome name="close" />
            <span>&nbsp;</span>
            IKKE ok
          </Button>
        </Row>
      </div>
    )
  }
}
