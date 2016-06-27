import React from 'react'
import { Button, Well } from 'react-bootstrap'

export default (actions) => {
  const style = require('./index.scss')
  const actionList = actions.map((action) => (
    <Button key={action.id} onClick={action.func} block>{action.description}</Button>
  ))
  return (
    <Well className={style.actionList}>
      {actionList}
    </Well>
  )
}
