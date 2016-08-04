import React, { Component, PropTypes } from 'react'
import { Table, ControlLabel, Grid, Row, Col, Button } from 'react-bootstrap'
import FontAwesome from 'react-fontawesome'


export default class NodeLeftMenuComponent extends Component {
  static propTypes = {
    id: PropTypes.string.isRequired,
    translate: PropTypes.func.isRequired,
    onClickNewNode: PropTypes.func.isRequired,
    objectsOnNode: PropTypes.number.isRequired,
    totalObjectCount: PropTypes.number.isRequired,
    underNodeCount: PropTypes.number.isRequired,
    onClickProperties: PropTypes.func.isRequired,
    onClickObservations: PropTypes.func.isRequired,
    onClickController: PropTypes.func.isRequired,
    onClickMoveNode: PropTypes.func.isRequired,
    onClickDelete: PropTypes.func.isRequired
  }
  render() {
    const {
      id,
      translate,
      onClickNewNode,
      objectsOnNode,
      totalObjectCount,
      underNodeCount,
      onClickProperties,
      onClickObservations,
      onClickController,
      onClickMoveNode,
      onClickDelete
    } = this.props
    const buttonLink = (type, icon, eventType) => {
      return (
        <div style={{ border: 'none', textAlign: 'center' }}>
          <Button
            bsStyle="link"
            id={`${id}_${type}`}
            onClick={(event) => eventType(event.target.value)}
            style={{ color: 'black' }}
          >
            <FontAwesome name={icon} style={{ padding: '2px' }} />
            <br />
            {translate(`musit.leftMenu.node.${type}`)}
          </Button>
        </div>
      ) }
    const showCount = (type, typeText) => {
      return (
        <div style={{ border: 'none', textAlign: 'center' }}>
            {translate(`musit.leftMenu.node.${typeText}`)}
            <br />
            <ControlLabel id={`${id}_${typeText}`}>{type}</ControlLabel>
        </div>
      )
    }
    const newButton = (identity) => {
      return (
        <div  style={{ border: 'none', textAlign: 'center' }}>
          <Button
            id={`${identity}_newNode`}
            onClick={(event) => onClickNewNode(event.target.value)}
            style={{ width: '100%', textAlign: 'left'}}
          >
            <FontAwesome name="plus-circle" style={{ padding: '2px' }} />
            {translate('musit.leftMenu.node.newNode')}
          </Button>
        </div>
      )
    }
    return (
      <div>
        {newButton(id)}
        <hr />
        {showCount(objectsOnNode, 'objectsOnNode')}
        {showCount(totalObjectCount, 'totalObjectCount')}
        {showCount(underNodeCount, 'underNodeCount')}
        <hr />
        {buttonLink('properties', 'cog', onClickProperties)}
        {buttonLink('observations', 'eye', onClickObservations)}
        {buttonLink('controller', 'user-secret', onClickController)}
        {buttonLink('moveNode', 'truck', onClickMoveNode)}
        {buttonLink('delete', 'trash-o', onClickDelete)}
      </div>
    )
  }
}
