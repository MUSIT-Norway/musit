

import React, { Component, PropTypes } from 'react'
import { Row, Col, Button, Table, FormGroup } from 'react-bootstrap'
import FontAwesome from 'react-fontawesome'
import { MusitField } from '../formfields'

export default class NodeGrid extends Component {
  static propTypes = {
    id: PropTypes.string.isRequired,
    translate: PropTypes.func.isRequired,
    tableData: PropTypes.arrayOf(PropTypes.shape({
      name: PropTypes.string.isRequired,
      type: PropTypes.string.isRequired,
      objectCount: PropTypes.number,
      totalObjectCount: PropTypes.number,
      nodeCount: PropTypes.number
    }))
  }

  render() {
    const { id, translate } = this.props
    return (
      <FormGroup>
        <div>
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
              {this.props.tableData.map((c, i) =>
                <tr key={i} id={`${id}_${c.name}_${c.type}`} >
                  <td id={`${id}_${c.name}_${c.type}_nodeName`}>
                    <FontAwesome name="folder" />
                    {` ${c.name}`}
                  </td>
                  <td id={`${id}_${c.name}_${c.type}_nodeType`}>
                    {c.type}
                  </td>
                  <td id={`${id}_${c.name}_${c.type}_objectCount`}>
                    {c.objectCount}
                  </td>
                  <td id={`${id}_${c.name}_${c.type}_totalObjectCount`}>
                    {c.totalObjectCount}
                  </td>
                  <td id={`${id}_${c.name}_${c.type}_nodeCount`}>
                    {c.nodeCount}
                  </td>
                  <td id={`${id}_${c.name}_${c.type}_eye`}>
                    <FontAwesome name="eye" />
                  </td>
                  <td id={`${id}_${c.name}_${c.type}_search`}>
                    <FontAwesome name="search" />
                  </td>
                  <td id={`${id}_${c.name}_${c.type}_truck`}>
                    <FontAwesome name="truck" />
                  </td>
                  <td id={`${id}_${c.name}_${c.type}_shoppingCart`}>
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
