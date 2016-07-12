

import React, { Component, PropTypes } from 'react'
import { Row, Col, Button, Table, FormGroup } from 'react-bootstrap'
import FontAwesome from 'react-fontawesome'
import { MusitField } from '../formfields'

export default class NodeGrid extends Component {
  static propTypes = {
    id: PropTypes.string.isRequired,
    translate: PropTypes.func.isRequired,
    tableData: PropTypes.arrayOf(PropTypes.shape({
      nodeName: PropTypes.string.isRequired,
      nodeType: PropTypes.string.isRequired,
      objectCount: PropTypes.number.isRequired,
      totalObjectCount: PropTypes.number.isRequired,
      nodeCount: PropTypes.number.isRequired
    }))
  }

  render() {
    const { id, translate } = this.props
    const objectGrid = (c) => {
      return (
        <tr id={`${id}_${c.nodeName}_${c.nodeType}`} >
          <td id={`${id}_${c.nodeName}_${c.nodeType}_nodeName`}>
            <FontAwesome name="folder" />
            {` ${c.nodeName}`}
          </td>
          <td id={`${id}_${c.nodeName}_${c.nodeType}_nodeType`}>
            {c.nodeType}
          </td>
          <td id={`${id}_${c.nodeName}_${c.nodeType}_objectCount`}>
            {c.objectCount}
          </td>
          <td id={`${id}_${c.nodeName}_${c.nodeType}_totalObjectCount`}>
            {c.totalObjectCount}
          </td>
          <td id={`${id}_${c.nodeName}_${c.nodeType}_nodeCount`}>
            {c.nodeCount}
          </td>
          <td id={`${id}_${c.nodeName}_${c.nodeType}_eye`}>
            <FontAwesome name="eye" />
          </td>
          <td id={`${id}_${c.nodeName}_${c.nodeType}_search`}>
            <FontAwesome name="search" />
          </td>
          <td id={`${id}_${c.nodeName}_${c.nodeType}_truck`}>
            <FontAwesome name="truck" />
          </td>
          <td id={`${id}_${c.nodeName}_${c.nodeType}_shoppingCart`}>
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
                  {translate('musit.grid.node.nodeName')}
                </th>
                <th>
                  {translate('musit.grid.node.nodeType')}
                </th>
                <th>
                  {translate('musit.grid.node.objectCount')}
                </th>
                <th>
                  {translate('musit.grid.node.totalObjectCount')}
                </th>
                <th>
                  {translate('musit.grid.node.nodeCount')}
                </th>
                <th>
                </th>
                <th>
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
