import React, { Component, PropTypes } from 'react'
import { Grid, Row, Col, Button, Checkbox } from 'react-bootstrap'
import FontAwesome from 'react-fontawesome'


export default class ObservationControlComponent extends Component {
  static propTypes = {
    id: PropTypes.string.isRequired,
    translate: PropTypes.func.isRequired,
    selectObservation: PropTypes.bool,
    selectControl: PropTypes.bool,
    onClickNewObservation: PropTypes.func.isRequired,
    onClickNewControl: PropTypes.func.isRequired,
    onClickSelectObservation: PropTypes.func.isRequired,
    onClickSelectControl: PropTypes.func.isRequired,
  }
  render() {
    const { id, translate, onClickSelectObservation, onClickSelectControl, onClickNewControl, onClickNewObservation } = this.props
    const getFormat = (code) => {
      return (
        <Row>
          <Col sm={8} smOffset={2}>
            {code}
          </Col>
        </Row>
      ) }
    const getTranslate = (term) => (translate(`musit.leftmenu.observationControl.${term}`))
    const buttonLogic = (type, eventType) => {
      return (
        getFormat(
          <Button
            id={`${id}_${type}`}
            onClick={(event) => eventType(event.target.value)}
          >
            <FontAwesome name="plus-circle" style={{ padding: '2px' }} />
            {getTranslate(type)}
          </Button>
        )
    ) }
    const checkboxLogic = (type, eventType) => {
      return (
        getFormat(
          <Checkbox
            id={`${id}_${type}`}
            checked={this.props[type]}
            onChange={(event) => eventType(event.target.checked)}
          >
            {getTranslate(type)}
          </Checkbox>
        )
    ) }
    return (
      <div>
        <main>
          <Grid>
            {buttonLogic('newObservation', onClickNewObservation)}
            <br />
            {buttonLogic('newControl', onClickNewControl)}
            <br />
            <br />
            <br />
            {getFormat(getTranslate('show'))}
            {checkboxLogic('selectControl', onClickSelectControl)}
            {checkboxLogic('selectObservation', onClickSelectObservation)}
          </Grid>
        </main>
      </div>
    )
  }
}
