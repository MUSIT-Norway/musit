import React, { Component, PropTypes } from 'react'
import { Table, FormGroup } from 'react-bootstrap'
import FontAwesome from 'react-fontawesome'

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
    // const { id } = this.props
    const objectGrid = (c) => {
      return (
        <tr id={c.type} >
          <td>
            <FontAwesome name="rebel" />
            {` ${c.museumsNumber}`}
          </td>
          <td>
            {c.uNumber}
          </td>
          <td>
            {c.term}
          </td>
          <td>
            <FontAwesome name="truck" />
          </td>
          <td>
            <FontAwesome name="shopping-cart" />
          </td>
        </tr>
      )
    }
    return (
      <FormGroup>
        <div>
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
