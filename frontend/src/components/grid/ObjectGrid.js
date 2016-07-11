import React, { Component, PropTypes } from 'react'
import { Row, Col, Button, Table, FormGroup } from 'react-bootstrap'
import FontAwesome from 'react-fontawesome'
import { MusitField } from '../formfields'

export default class ObjectGrid extends Component {
  static propTypes = {
    id: PropTypes.string.isRequired,
    translate: PropTypes.func.isRequired,
    controls: PropTypes.arrayOf(PropTypes.shape({
      type: PropTypes.string.isRequired,
      ok: PropTypes.bool.isRequired
    }))
  }

  render() {
    const { id } = this.props
    const objectGrid = (c) => {
      return (
        <tr id={`${id}_${c.museumsNumber}_${c.uNumber}`} >
          <td id={`${id}_${c.museumsNumber}_${c.uNumber}_museumNumber`}>
            <FontAwesome name="rebel" />
            {` ${c.museumsNumber}`}
          </td>
          <td id={`${id}_${c.museumsNumber}_${c.uNumber}_uNumber`}>
            {c.uNumber}
          </td>
          <td id={`${id}_${c.museumsNumber}_${c.uNumber}_term`}>
            {c.term}
          </td>
          <td id={`${id}_${c.museumsNumber}_${c.uNumber}_truck`}>
            <FontAwesome name="truck" />
          </td>
          <td id={`${id}_${c.museumsNumber}_${c.uNumber}_shoppingCart`}>
            <FontAwesome name="shopping-cart" />
          </td>
        </tr>
      )
    }
    return (
      <FormGroup>
        <div>
          <Row>
            <Col sm={6} />
            <Col sm={3}>
              <MusitField
                id={`${id}_searchMusitField`}
                addOnPrefix={'\u2315'}
                placeHolder="Filterer i liste"
                value="" validate="text"
              />
            </Col>
            <Col sm={3}>
              <Button id={`${id}_Nodes`}>Noder</Button>
              <Button id={`${id}_Objects`}>Objeckter</Button>
            </Col>
          </Row>
          <Table responsive hover condensed>
            <thead>
              <tr>
                <th>
                  Museumsnr
                </th>
                <th>
                  Unr
                </th>
                <th>
                  Term
                </th>
                <th>
                </th>
                <th>
                </th>
              </tr>
            </thead>
            <tbody>
              {this.props.controls.map(c =>
                objectGrid(c)
              )}
            </tbody>
          </Table>
        </div>
      </FormGroup>
    )
  }
}
