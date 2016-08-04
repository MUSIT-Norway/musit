import React, { Component, PropTypes } from 'react'
import { Grid, Button, Checkbox } from 'react-bootstrap'
import FontAwesome from 'react-fontawesome'

export default class ObservationControlComponent extends Component {
  static propTypes = {
    id: PropTypes.number.isRequired,
    translate: PropTypes.func.isRequired,
    selectObservation: PropTypes.bool,
    selectControl: PropTypes.bool,
    onClickNewObservation: PropTypes.func.isRequired,
    onClickNewControl: PropTypes.func.isRequired,
    onClickSelectObservation: PropTypes.func.isRequired,
    onClickSelectControl: PropTypes.func.isRequired
  }

  render() {
    const { id, translate, onClickSelectObservation, onClickSelectControl, onClickNewControl, onClickNewObservation } = this.props
    const getTranslate = (term) => (translate(`musit.leftmenu.observationControl.${term}`))
    const buttonLogic = (type, eventType) => {
      return (
        <Button
          id={`${id}_${type}`}
          onClick={(event) => eventType(event.target.value)}
          style={{ width: '100%', textAlign: 'left'}}
        >
          <FontAwesome name="plus-circle" style={{ padding: '2px' }} />
          {getTranslate(type)}
        </Button>
      )
    }
    return (
      <div>
        {buttonLogic('newObservation', onClickNewObservation)}
        {buttonLogic('newControl', onClickNewControl)}
      </div>
    )
  }
}
