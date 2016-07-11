

import React, { Component, PropTypes } from 'react'
import { Row, Col, Button, Table, FormGroup } from 'react-bootstrap'
import FontAwesome from 'react-fontawesome'
import { MusitField } from '../formfields'

export default class NodeGrid extends Component {
  static propTypes = {
    id: PropTypes.string.isRequired,
    translate: PropTypes.func.isRequired,
    tableData: PropTypes.arrayOf(PropTypes.shape({
      museumsNumber: PropTypes.string.isRequired,
      uNumber: PropTypes.string.isRequired,
      term: PropTypes.string
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
            <Col xs={0} sm={5} />
            <Col xs={12} sm={3}>
              <MusitField
                id={`${id}_searchMusitField`}
                addOnPrefix={'\u2315'}
                placeHolder="Filterer i liste"
                value="" validate="text"
              />
            </Col>
            <Col xs={12} sm={4}>
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
              {this.props.tableData.map(c =>
                objectGrid(c)
              )}
            </tbody>
          </Table>
        </div>
      </FormGroup>
    )
  }
}
