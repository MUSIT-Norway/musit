import React, { Component } from 'react'
import { Grid, Row, Col } from 'react-bootstrap'

export default class PickListComponent extends Component {
  static propTypes = {
    picks: React.PropTypes.array.isRequired,
    iconRendrer: React.PropTypes.func.isRequired,
    labelRendrer: React.PropTypes.func.isRequired
  }

  render() {
    const { picks, iconRendrer, labelRendrer } = this.props
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
        {pickRows}
      </Grid>
    )
  }

}
