import React, { Component } from 'react'
import { Table } from 'react-bootstrap'
import FontAwesome from 'react-fontawesome'
import ActionListPopupContainer from './ActionListPopupContainer'

export default class PickListComponent extends Component {
  static propTypes = {
    picks: React.PropTypes.array.isRequired,
    marked: React.PropTypes.array.isRequired,
    actions: React.PropTypes.array.isRequired,
    iconRendrer: React.PropTypes.func.isRequired,
    labelRendrer: React.PropTypes.func.isRequired,
    onToggleMarked: React.PropTypes.func.isRequired,
    showActionDialog: React.PropTypes.bool.isRequired,
    onCloseActionDialog: React.PropTypes.func.isRequired
  }

  render() {
    const style = require('./index.scss')
    const {
      picks,
      marked,
      iconRendrer,
      labelRendrer,
      onToggleMarked,
      actions,
      showActionDialog,
      onCloseActionDialog
    } = this.props
    console.log(actions)
    const actionsComponent = (
      <span>
        <FontAwesome className={style.normalAction} name="play-circle" />
        <FontAwesome className={style.warningAction} name="remove" />
      </span>
    )
    const pickRows = picks.map((pick) => {
      const checkSymbol = (marked.indexOf(pick.id) >= 0) ? 'check-square-o' : 'square-o'
      const rowStyleClass = checkSymbol === 'check-square-o' ? 'highlight' : ''
      return (
        <tr key={pick.id} onClick={(e) => onToggleMarked(e, pick.id)} className={rowStyleClass}>
          <td className={style.icon}>{iconRendrer()}</td>
          <td className={style.label}>{labelRendrer(pick)}<FontAwesome className={style.infoAction} name="info-circle" /></td>
          <td className={style.select}><FontAwesome className={style.normalAction} name={checkSymbol} /></td>
          <td className={style.actions}>{actionsComponent}</td>
        </tr>
      )
    })

    return (
      <div>
        <ActionListPopupContainer show={showActionDialog} marked={marked} actions={actions} onClose={onCloseActionDialog} />
        <Table responsive striped condensed hover>
          <tbody>
            {pickRows}
          </tbody>
        </Table>
      </div>
    )
  }

}
