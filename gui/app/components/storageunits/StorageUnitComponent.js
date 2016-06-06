/**
 * Created by steinaol on 5/27/16.
 */

import React, { Component } from 'react';
import TextField from '../../components/musittextfield';
import MusitDropDown from '../../components/musitdropdownfield';
import { Panel, Form, Grid, Row, Col, } from 'react-bootstrap'

export default class StorageUnitComponent extends Component {
  static propTypes = {
    unit: React.PropTypes.shape({
      name: React.PropTypes.string,
      areal1: React.PropTypes.string,
      areal2: React.PropTypes.string,
      height1: React.PropTypes.string,
      height2: React.PropTypes.string,
      type: React.PropTypes.string,
    }),
    updateType: React.PropTypes.func.isRequired,
    updateName: React.PropTypes.func.isRequired,
    updateHeight1: React.PropTypes.func.isRequired,
    updateHeight2: React.PropTypes.func.isRequired,
    updateAreal1: React.PropTypes.func.isRequired,
    updateAreal2: React.PropTypes.func.isRequired,
  }

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

    this.areal = {
      controlId: 'areal1',
      controlId2: 'areal2',
      labelText: 'Areal (fra-til)',
      tooltip: 'Areal (fra - til)',
      valueType: 'text',
      placeHolderText: 'enter areal 1 here',
      placeHolderText2: 'enter areal 2 here',
      valueText: () => this.props.unit.areal1,
      valueText2: () => this.props.unit.areal2,
      validationState: () => StorageUnitComponent.validateNumber(this.props.unit.areal1),
      onChange: (areal1) => this.props.updateAreal1(areal1),
      onChange2: (areal2) => this.props.updateAreal2(areal2)
    }

    this.hoyde = {
      controlId: 'hoyde1',
      controlId2: 'hoyde2',
      labelText: 'Høyde(fra-til)',
      tooltip: 'Høyde (fra - til)',
      valueType: 'text',
      placeHolderText: 'enter høyde 1 here',
      placeHolderText2: 'enter høyde 2 here',
      valueText: () => this.props.unit.height1,
      valueText2: () => this.props.unit.height2,
      validationState: () => StorageUnitComponent.validateNumber(this.props.unit.height1),
      validationState2: () => StorageUnitComponent.validateNumber(this.props.unit.height2),
      onChange: (height1) => this.props.updateHeight1(height1),
      onChange2: (height2) => this.props.updateHeight2(height2)
    }


    this.type = {
      controlId: 'storageUnitType',
      labelText: 'Type',
      items: ['Lagringsenhet', 'Rom', 'Bygning', 'Organisasjon'],
      valueType: 'text',
      tooltip: 'Type lagringsenhet',
      placeHolderText: 'velg type here',
      valueText: () => this.props.unit.type,
      validationState: () => StorageUnitComponent.validateString(this.props.unit.type),
      onChange: (type) => this.props.updateType(type)
    }
    this.name = {
      controlId: 'name',
      labelText: 'Navn',
      tooltip: 'Navn',
      placeHolderText: 'enter name here',
      valueType: 'text',
      valueText: () => this.props.unit.name,
      validationState: () => StorageUnitComponent.validateString(this.props.unit.name),
      onChange: (name) => this.props.updateName(name)
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
