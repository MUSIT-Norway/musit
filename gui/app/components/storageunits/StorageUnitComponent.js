/**
 * Created by steinaol on 5/27/16.
 */

import React, { Component } from 'react';
import TextField from '../../components/musittextfield';
import MusitDropDown from '../../components/musitdropdownfield';
import { Panel, Form, Grid, Row, Col, } from 'react-bootstrap'

export default class StorageUnitComponent extends Component {
  static validateString(value, minimumLength = 3, maximumLength = 20) {
    const isSomething = value.length >= minimumLength
    const isValid = isSomething ? 'success' : null
    return value.length > maximumLength ? 'error' : isValid
  }

  static validateNumber(value, minimumLength = 1) {
    const isSomething = value.length >= minimumLength
    const isValid = isSomething ? 'success' : null
    return isSomething && isNaN(value) ? 'error' : isValid
  }

  constructor(props) {
    super(props)

    this.state = {
      storageunit: {
        type: 'Lagringsenhet',
        name: 'navn',
        areal1: '4',
        areal2: '9',
        hoyde1: '2',
        hoyde2: '4'
      }
    }

    const clazz = this

    this.areal = {
      controlId: 'areal1',
      controlId2: 'areal2',
      labelText: 'Areal (fra-til)',
      tooltip: 'Areal (fra - til)',
      valueType: 'text',
      placeHolderText: 'enter areal 1 here',
      placeHolderText2: 'enter areal 2 here',
      valueText: () => clazz.state.storageunit.areal1,
      valueText2: () => clazz.state.storageunit.areal2,
      validationState: () => StorageUnitComponent.validateNumber(clazz.state.storageunit.areal1),
      onChange: (areal1) => clazz.setState({ storageunit: { ...this.state.storageunit, areal1 } }),
      onChange2: (areal2) => clazz.setState({ storageunit: { ...this.state.storageunit, areal2 } })
    }

    this.hoyde = {
      controlId: 'hoyde1',
      controlId2: 'hoyde2',
      labelText: 'Høyde(fra-til)',
      tooltip: 'Høyde (fra - til)',
      valueType: 'text',
      placeHolderText: 'enter høyde 1 here',
      placeHolderText2: 'enter høyde 2 here',
      valueText: () => clazz.state.storageunit.hoyde1,
      valueText2: () => clazz.state.storageunit.hoyde2,
      validationState: () => StorageUnitComponent.validateNumber(clazz.state.storageunit.hoyde1),
      validationState2: () => StorageUnitComponent.validateNumber(clazz.state.storageunit.hoyde2),
      onChange: (hoyde1) => clazz.setState({ storageunit: { ...this.state.storageunit, hoyde1 } }),
      onChange2: (hoyde2) => clazz.setState({ storageunit: { ...this.state.storageunit, hoyde2 } })
    }


    this.type = {
      controlId: 'storageUnitType',
      labelText: 'Type',
      items: ['Lagringsenhet', 'Room', 'Bui666lding'],
      valueType: 'text',
      placeHolderText: 'velg type here',
      valueText: () => clazz.state.storageunit.type,
      validationState: () => StorageUnitComponent.validateString(clazz.state.storageunit.type),
      onChange: (type) => clazz.setState({ storageunit: { ...clazz.state.storageunit, type } })
    }
    this.name = {
      controlId: 'name',
      labelText: 'Navn',
      tooltip: 'Navn',
      placeHolderText: 'enter name here',
      valueType: 'text',
      valueText: () => clazz.state.storageunit.name,
      validationState: () => StorageUnitComponent.validateString(clazz.state.storageunit.name),
      onChange: (name) => clazz.setState({ storageunit: { ...clazz.state.storageunit, name } })
    }
  }

  render() {
    return (
        <div>
            <main>
                <Panel>
                    <Grid>
                        <Row styleClass="row-centered">
                            <Col md={6}>
                                <Form horizontal>
                                    <MusitDropDown {...this.type} />
                                </Form>
                            </Col>
                            <Col md={6}>
                                <Form horizontal>
                                    <TextField {...this.name} />
                                </Form>
                            </Col>
                        </Row>
                        <Row styleClass="row-centered">
                            <Col md={6}>
                                <Form horizontal>
                                    <TextField {...this.areal} />
                                </Form>
                            </Col>

                            <Col md={6}>
                                <Form horizontal>
                                    <TextField {...this.hoyde} />
                                </Form>
                            </Col>

                        </Row>
                    </Grid>
                </Panel>
            </main>
        </div>
        );
  }
}
