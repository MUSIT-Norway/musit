import React, { Component } from 'react'
import { Table } from 'react-bootstrap'
import FontAwesome from 'react-fontawesome'

export default class PickListComponent extends Component {
  static propTypes = {
    picks: React.PropTypes.array.isRequired,
    marked: React.PropTypes.array.isRequired,
    iconRendrer: React.PropTypes.func.isRequired,
    labelRendrer: React.PropTypes.func.isRequired,
    onToggleMarked: React.PropTypes.func.isRequired
  }

  render() {
    const style = require('./index.scss')
    const { picks, marked, iconRendrer, labelRendrer, onToggleMarked } = this.props
    const actions = (
      <span>
        <FontAwesome className={style.normalAction} name="play-circle" />
        <FontAwesome className={style.warningAction} name="remove" />
      </span>
    )
    const pickRows = picks.map((pick) => {
      const checkSymbol = (marked.indexOf(pick.id) >= 0) ? 'check-square-o' : 'square-o'
      return (
        <tr key={pick.id} onClick={(e) => onToggleMarked(e, pick.id)}>
          <td className={style.icon}>{iconRendrer()}</td>
          <td className={style.label}>{labelRendrer(pick)}</td>
          <td className={style.select}><FontAwesome className={style.normalAction} name={checkSymbol} /></td>
          <td className={style.actions}>{actions}</td>
        </tr>
      )
    })

    return (
      <Table responsive striped condensed hover>
        <tbody>
          {pickRows}
        </tbody>
      </Table>
    )
  }

}
