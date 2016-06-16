import React, { Component } from 'react'
import { Modal, Button } from 'react-bootstrap'
import OrganizationContainer from './OrganizationContainer'

export default class OrganizationPopupContainer extends Component {
  static propTypes = {
    org: React.PropTypes.shape({
      fn: React.PropTypes.string,
      nickname: React.PropTypes.string,
      tel: React.PropTypes.string,
      web: React.PropTypes.string,
    }),
    show: React.PropTypes.bool.isRequired,
    onClose: React.PropTypes.func.isRequired,
    onSave: React.PropTypes.func.isRequired,
    updateFN: React.PropTypes.func.isRequired,
    updateNickname: React.PropTypes.func.isRequired,
    updateTel: React.PropTypes.func.isRequired,
    updateWeb: React.PropTypes.func.isRequired
  }

  render() {
    const { onSave, show, onClose, org, updateFN, updateNickname, updateWeb, updateTel } = this.props
    return (
      <Modal show={show} onHide={onClose}>
        <Modal.Header closeButton>
          <Modal.Title>Opprett organisasjon</Modal.Title>
        </Modal.Header>

        <Modal.Body>
          <OrganizationContainer
            org={org}
            updateFN={updateFN}
            updateNickname={updateNickname}
            updateWeb={updateWeb}
            updateTel={updateTel}
          />
        </Modal.Body>

        <Modal.Footer>
          <Button onClick={onClose}>Avbryt</Button>
          <Button bsStyle="primary" onClick={onSave}>Opprett</Button>
        </Modal.Footer>
      </Modal>
    )
  }
}
