

import React, { Component, PropTypes } from 'react'
import { Table, FormGroup } from 'react-bootstrap'
import FontAwesome from 'react-fontawesome'

export default class NodeGrid extends Component {
  static propTypes = {
    id: PropTypes.number.isRequired,
    translate: PropTypes.func.isRequired,
    tableData: PropTypes.arrayOf(PropTypes.shape({
      id: PropTypes.number.isRequired,
      name: PropTypes.string.isRequired,
      type: PropTypes.string.isRequired,
      objectCount: PropTypes.number,
      totalObjectCount: PropTypes.number,
      nodeCount: PropTypes.number
    })),
    onPick: PropTypes.func.isRequired,
    onClick: PropTypes.func.isRequired
  }

  render() {
    const { id, translate, tableData } = this.props

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
              {tableData.map((c, i) =>
                <tr key={i} id={`${id}_${c.name}_${c.type}`} >
                  <td id={`${id}_${c.name}_${c.type}_nodeName`}>
                    <a
                      href=""
                      onClick={(e) => {
                        e.preventDefault()
                        this.props.onClick(c)
                      }}
                    >
                      <FontAwesome name="folder" />
                      {` ${c.name}`}
                    </a>
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
                    <a
                      href=""
                      onClick={(e) => {
                        e.preventDefault()
                        this.props.onPick(c)
                      }}
                    >
                      <FontAwesome name="shopping-cart" />
                    </a>
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
