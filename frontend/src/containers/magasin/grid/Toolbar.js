import React from 'react'
import { Row, Button, Col } from 'react-bootstrap'
import { MusitField } from '../../../components/formfields'

export default class Toolbar extends React.Component {
  render() {
    return (
      <Row>
        <Col xs={6}>
          <MusitField
            id={"search"}
            addOnPrefix={'\u2315'}
            placeHolder="Filtrer i liste"
            value=""
            validate="text"
          />
        </Col>
        <Col xs={6}>
          <Button>Noder</Button>
          <Button>Objekter</Button>
        </Col>
      </Row>
    )
  }
}
