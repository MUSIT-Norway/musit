import React, { Component, PropTypes } from 'react';
import FontAwesome from 'react-fontawesome';
import { Button, Row, Col } from 'react-bootstrap';

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
          <Col xs={2} />
          <Col xs={10}>
            <label>
              {label}
            </label>
          </Col>
        </Row>
        <Row>
          <Col xs={2}>
            {value != null ?
              <FontAwesome
                name={value ? 'check' : 'times'}
                size={'2x'}
                style={value ? { color: 'green', padding: '2px' } : { color: 'red', padding: '2px' }}
              /> :
              null}
          </Col>
          <Col xs={10}>
            <Button
              className={value ? style.buttonpaddingtrue : style.buttonpaddingfalse}
              onClick={this.props.updatevalueOK}
            >
              <FontAwesome name="check" />
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
          </Col>
        </Row>
      </div>
    )
  }
}
