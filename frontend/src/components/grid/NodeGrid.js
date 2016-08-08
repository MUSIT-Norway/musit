

import React, { Component, PropTypes } from 'react'
import { Row, Col, Button, Table, FormGroup } from 'react-bootstrap'
import FontAwesome from 'react-fontawesome'
import { MusitField } from '../formfields'

export default class NodeGrid extends Component {
  static propTypes = {
    id: PropTypes.string.isRequired,
    translate: PropTypes.func.isRequired,
    loadNode: PropTypes.func.isRequired,
    tableData: PropTypes.arrayOf(PropTypes.shape({
      id: PropTypes.number.isRequired,
      name: PropTypes.string.isRequired,
      storageType: PropTypes.string.isRequired,
      objectCount: PropTypes.number.isRequired,
      totalObjectCount: PropTypes.number.isRequired,
      nodeCount: PropTypes.number.isRequired
    }))
  }

  render() {
    const { id, translate, loadNode, tableData } = this.props

    return (
      <FormGroup>
        <div>
          <Row>
            <Col xs={0} sm={5} />
            <Col xs={12} sm={3}>
              <MusitField
                id={`${id}_searchMusitField`}
                addOnPrefix={'\u2315'}
                placeHolder="Filtrer i liste"
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
                <th />
                <th />
                <th />
                <th />
              </tr>
            </thead>
            <tbody>
              {tableData.map(c =>
                <tr id={`${id}_${c.name}_${c.storageType}`} >
                  <td id={`${id}_${c.name}_${c.storageType}_name`} onClick={() => loadNode(c.id)}>
                    <FontAwesome name="folder" />
                    {c.name}
                  </td>
                  <td id={`${id}_${c.name}_${c.storageType}_storageType`}>
                    {c.storageType}
                  </td>
                  <td id={`${id}_${c.name}_${c.storageType}_objectCount`}>
                    {c.objectCount}
                  </td>
                  <td id={`${id}_${c.name}_${c.storageType}_totalObjectCount`}>
                    {c.totalObjectCount}
                  </td>
                  <td id={`${id}_${c.name}_${c.storageType}_nodeCount`}>
                    {c.nodeCount}
                  </td>
                  <td id={`${id}_${c.name}_${c.storageType}_eye`}>
                    <FontAwesome name="eye" />
                  </td>
                  <td id={`${id}_${c.name}_${c.storageType}_search`}>
                    <FontAwesome name="search" />
                  </td>
                  <td id={`${id}_${c.name}_${c.storageType}_truck`}>
                    <FontAwesome name="truck" />
                  </td>
                  <td id={`${id}_${c.name}_${c.storageType}_shoppingCart`}>
                    <FontAwesome name="shopping-cart" />
                  </td>
                </tr>
              )}
            </tbody>
          </Table>
        </div>
      </FormGroup>
    )
  }
}
