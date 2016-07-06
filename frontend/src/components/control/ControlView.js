import React, { Component, PropTypes } from 'react'
import { ControlLabel, Panel, FormGroup, Button, Col, Row } from 'react-bootstrap'
import { MusitField } from '../formfields'
import FontAwesome from 'react-fontawesome';

export default class ControlView extends Component {
  static propTypes = {
    id: PropTypes.string.isRequired,
    translate: PropTypes.func.isRequired,
    controls: PropTypes.arrayOf(PropTypes.shape({
      type: PropTypes.string.isRequired,
      ok: PropTypes.bool.isRequired
    }))
  }

  static iconMap = {
    temperature: 'asterisk',
    relativeHumidity: 'tint',
    lightCondition: 'sun-o',
    pest: 'bug',
    alcohol: 'percent'

  }

  constructor(props) {
    super(props);
    this.state = {
      temperature: {
        open: false
      },
      relativeHumidity: {
        open: false
      },
      lightCondition: {
        open: false
      },
      pest: {
        open: false
      },
      alcohol: {
        open: false
      }
    };
  }

  render() {
    const { id } = this.props
    const observation = (fontName, observationType) => {
      return (
        <Col xs={5} sm={5} >
          <FontAwesome name={fontName} />
          {` ${observationType}`}
        </Col>
    ) }
    const controlOk = (
      <Col xs={5} sm={5} >
        <FontAwesome name="check" />
        {`  ${this.props.translate('musit.texts.ok')}`}
      </Col>
    )
    const controlNotOk = (
      <Col xs={5} sm={5} >
        <FontAwesome name="close" />
        {`  ${this.props.translate('musit.texts.notOk')}`}
      </Col>
    )
    const downButton = (observationType) => {
      return (
        <Col xs={2} sm={2} >
          <Button
            id={`${id}_${observationType}_downButton`}
            onClick={() => this.setState({ [observationType]: { open: !this.state[observationType].open } })}
            bsStyle="link"
          >
            <FontAwesome name="sort-desc" />
          </Button>
        </Col>
      ) }
    const oneTableRow = (control) => {
      const { type, ok } = control
      return (
        <div>
          <Row>
            {observation(ControlView.iconMap[type], this.props.translate(`musit.controls.${type}`))}
            {ok ? controlOk : controlNotOk}
            {downButton(type)}
          </Row>
          <Row>
            <Panel collapsible expanded={this.state[type].open}>
              <Col sm={4} >
                <ControlLabel>label text</ControlLabel>
                <br />
                <MusitField id={`${id}_${type}_MusitField`} value="test user" validate="text" />
              </Col>
            </Panel>
          </Row>
        </div>
      ) }

    return (
      <FormGroup>
        {this.props.controls.map(c =>
          oneTableRow(c)
        )}
      </FormGroup>
    )
  }
}
