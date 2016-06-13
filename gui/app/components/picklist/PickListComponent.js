import React, { Component } from 'react'
import { Grid, Row, Col } from 'react-bootstrap'

export default class PickListComponent extends Component {
  static propTypes = {
    picks: React.PropTypes.array.isRequired,
    header: React.PropTypes.string.isRequired,
    iconRendrer: React.PropTypes.func.isRequired,
    labelRendrer: React.PropTypes.func.isRequired
  }

  render() {
    const { picks, header, iconRendrer, labelRendrer } = this.props
    const pickRows = picks.map((pick) => {
      return (
        <Row key={labelRendrer(pick)}>
          <Col md={1}>{iconRendrer()}</Col>
          <Col md={9}>{labelRendrer(pick)}</Col>
          <Col md={1}>[select]</Col>
          <Col md={1}>[actions]</Col>
        </Row>
      )
    })

    return (
      <Grid>
        <Row>
          <Col md={10}>{header}</Col><Col md={2}>[actions]</Col>
        </Row>
        {pickRows}
      </Grid>
    )
  }

}
