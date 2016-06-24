import React, { Component } from 'react'
import { Modal, Button } from 'react-bootstrap'
import actionListComponent from './ActionListComponent'

export default class OrganizationPopupContainer extends Component {
  static propTypes = {
    actions: React.PropTypes.array.isRequired,
    marked: React.PropTypes.array.isRequired,
    show: React.PropTypes.bool.isRequired,
    onClose: React.PropTypes.func.isRequired
  }

  render() {
    const { show, onClose, actions, marked } = this.props
    return (
      <Modal show={show} onHide={onClose}>
        <Modal.Header closeButton>
          <Modal.Title>Velg operasjon</Modal.Title>
        </Modal.Header>

        <Modal.Body>
          {actionListComponent(actions)}
          <span>Antal valgte noder: {marked.length}</span>
        </Modal.Body>

        <Modal.Footer>
          <Button onClick={onClose}>Avbryt</Button>
        </Modal.Footer>
      </Modal>
    )
  }
}
