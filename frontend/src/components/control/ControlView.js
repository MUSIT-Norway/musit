import React, { Component } from 'react'
import { ControlLabel, Panel, FormGroup, Button, Col, Row } from 'react-bootstrap'
import { MusitField } from '../formfields'
import FontAwesome from 'react-fontawesome';

export default class ControlView extends Component {
  constructor(...args) {
    super(...args);
    this.state = {
      open: false
    };
  }
  render() {
    const observation = () => {
      return (
        <Col xs={5} sm={5} >
          <FontAwesome name="asterisk" />
          {'  Temperatur'}
        </Col>
    ) }
    const oK = (
      <Col xs={5} sm={5} >
        <FontAwesome name="check" />
        {'  OK'}
      </Col>
    )
    const notOK = (
      <Col xs={5} sm={5} >
        <FontAwesome name="close" />
        {'  Ikke OK'}
      </Col>
    )
    const downButton = (
      <Col xs={2} sm={2} >
        <Button
          onClick={() => this.setState({ open: !this.state.open })}
          bsStyle="link"
        >
          <FontAwesome name="sort-desc" />
        </Button>
      </Col>
    )
    const oneTableRow = (booleanOk) => {
      return (
        <div>
          <FormGroup>
            <Row>
              {observation()}
              {booleanOk ? oK : notOK}
              {downButton}
            </Row>
            <Row>
              <Panel collapsible expanded={this.state.open}>
                <Col sm={4} >
                  <ControlLabel>label text</ControlLabel>
                  <br />
                  <MusitField id="2" value="test user" validate="text" />
                </Col>
              </Panel>
            </Row>
          </FormGroup>
        </div>
      ) }

    return (
      <FormGroup>
        <br />
        {oneTableRow()}
      </FormGroup>
    )
  }
}
