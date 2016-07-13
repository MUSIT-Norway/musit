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
    const { id, translate } = this.props
    return (
      <div>
        <main>
          <Grid>
            <Row>
              <Col sm={8} smOffset={2}>
                <br />
                <Button
                  id={`${id}_newObservation`}
                  onClick={(event) => this.props.onClickNewObservation(event.target.value)}
                >
                  <FontAwesome name="plus-circle" style={{ padding: '2px' }} />
                  {translate('musit.leftmenu.observationControl.newObservation')}
                </Button>
              </Col>
            </Row>
            <Row>
              <Col sm={8} smOffset={2}>
                <br />
                <Button
                  id={`${id}_newControl`}
                  onClick={(event) => this.props.onClickNewControl(event.target.value)}
                >
                  <FontAwesome name="plus-circle" style={{ padding: '2px' }} />
                  {translate('musit.leftmenu.observationControl.newControl')}
                </Button>
              </Col>
            </Row>
            <Row>
              <br />
              <br />
              <br />
              <br />
            </Row>
            <Row>
              <Col sm={8} smOffset={2}>
                {translate('musit.leftmenu.observationControl.show')}
              </Col>
            </Row>
            <Row>
              <Col sm={8} smOffset={2}>
                <Checkbox
                  id={`${id}_selectControl`}
                  checked={this.props.selectControl}
                  onChange={(event) => this.props.onClickSelectControl(event.target.checked)}
                >
                  {translate('musit.leftmenu.observationControl.selectControl')}
                </Checkbox>
              </Col>
            </Row>
            <Row>
              <Col sm={8} smOffset={2}>
                <Checkbox
                  id={`${id}_selectObservation`}
                  checked={this.props.selectObservation}
                  onChange={(event) => this.props.onClickSelectObservation(event.target.checked)}
                >
                  {translate('musit.leftmenu.observationControl.selectObservation')}
                </Checkbox>
              </Col>
            </Row>
          </Grid>
        </main>
      </div>
    )
  }
}
