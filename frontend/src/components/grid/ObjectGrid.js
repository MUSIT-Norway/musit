import React, { Component, PropTypes } from 'react'
import { Table, FormGroup } from 'react-bootstrap'
import FontAwesome from 'react-fontawesome'

export default class ObjectGrid extends Component {
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
    const { id, translate, tableData } = this.props
    return (
      <FormGroup>
        <div>
          <Table responsive hover condensed>
            <thead>
              <tr>
                <th>
                  {translate('musit.grid.object.museumsNumber')}
                </th>
                <th>
                  {translate('musit.grid.object.uNumber')}
                </th>
                <th>
                  {translate('musit.grid.object.term')}
                </th>
                <th />
                <th />
              </tr>
            </thead>
            <tbody>
              {tableData.map((c, i) =>
                <tr key={i} id={`${id}_${c.museumsNumber}_${c.uNumber}`} >
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
              )}
            </tbody>
          </Table>
        </div>
      </FormGroup>
    )
  }
}
