/**
 * Created by steinaol on 5/27/16.
 */

import React, { Component } from 'react';
import TextField from '../../components/musittextfield';
import { Panel, Form, Grid, Row, Col, } from 'react-bootstrap'

export default class EnvironmentRequirementComponent extends Component {
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
      environmentRequirement: {
        temperature: '',
        temperatureTolerance: '',
        relativeHumidity: '',
        relativeHumidityTolerance: '',
        inertAir: '',
        inertAirTolerance: '',
        renhold: '',
        lightCondition: '',
        comments: ''
      }
    }

    const clazz = this

    this.temperature = {
      controlId: 'temperature',
      labelText: 'Temperature',
      placeHolderText: 'C',
      tooltip: 'Temperature',
      valueType: 'text',
      valueText: () => clazz.state.environmentRequirement.temperature,
      validationState: () => EnvironmentRequirementComponent.validateNumber(clazz.state.environmentRequirement.temperature),
      onChange: (temperature) => clazz.setState({ environmentRequirement: { ...this.state.environmentRequirement, temperature } })
    }
    this.temperatureTolerance = {
      controlId: 'temperatureTolerance',
      labelText: '+/-',
      placeHolderText: 'int',
      tooltip: 'Temperature Tolerance',
      valueType: 'text',
      valueText: () => clazz.state.environmentRequirement.temperatureTolerance,
      validationState: () => EnvironmentRequirementComponent.validateString(clazz.state.environmentRequirement.temperatureTolerance),
      onChange: (temperatureTolerance) => clazz.setState({ environmentRequirement: { ...clazz.state.environmentRequirement, temperatureTolerance } })
    }

    this.relativeHumidity = {
      controlId: 'relativeHumidity',
      labelText: 'Relativ Humidity',
      placeHolderText: '%',
      tooltip: 'Relativ Humidity',
      valueType: 'text',
      valueText: () => clazz.state.environmentRequirement.relativeHumidity,
      validationState: () => EnvironmentRequirementComponent.validateNumber(clazz.state.environmentRequirement.relativeHumidity),
      onChange: (relativeHumidity) => clazz.setState({ environmentRequirement: { ...this.state.environmentRequirement, relativeHumidity } })
    }
    this.relativeHumidityTolerance = {
      controlId: 'relativeHumidityTolerance',
      labelText: '+/-',
      placeHolderText: 'int',
      tooltip: 'Relativ Humidity Tolerance',
      valueType: 'text',
      valueText: () => clazz.state.environmentRequirement.relativeHumidityTolerance,
      validationState: () => EnvironmentRequirementComponent.validateString(clazz.state.environmentRequirement.relativeHumidityTolerance),
      onChange: (relativeHumidityTolerance) => clazz.setState({ environmentRequirement: { ...clazz.state.environmentRequirement, relativeHumidityTolerance } })
    }

    this.inertAir = {
      controlId: 'inertAir1',
      labelText: 'Inert Air',
      placeHolderText: '% O2',
      tooltip: 'Inert Air',
      valueType: 'text',
      valueText: () => clazz.state.environmentRequirement.inertAir,
      validationState: () => EnvironmentRequirementComponent.validateNumber(clazz.state.environmentRequirement.inertAir),
      onChange: (inertAir) => clazz.setState({ environmentRequirement: { ...this.state.environmentRequirement, inertAir } })
    }
    this.inertAirTolerance = {
      controlId: 'inertAirTolerance',
      labelText: '+/-',
      placeHolderText: 'int',
      tooltip: 'Inert Air Tolerance',
      valueType: 'text',
      valueText: () => clazz.state.environmentRequirement.inertAirTolerance,
      validationState: () => EnvironmentRequirementComponent.validateString(clazz.state.environmentRequirement.inertAirTolerance),
      onChange: (inertAirTolerance) => clazz.setState({ environmentRequirement: { ...clazz.state.environmentRequirement, inertAirTolerance } })
    }

    this.renhold = {
      controlId: 'renhold',
      labelText: 'Renhold',
      placeHolderText: '',
      tooltip: 'Renhold',
      valueType: 'text',
      valueText: () => clazz.state.environmentRequirement.renhold,
      validationState: () => EnvironmentRequirementComponent.validateString(clazz.state.environmentRequirement.renhold),
      onChange: (renhold) => clazz.setState({ environmentRequirement: { ...clazz.state.environmentRequirement, renhold } })
    }

    this.lightCondition = {
      controlId: 'lightCondition',
      labelText: 'LightCondition',
      placeHolderText: '',
      valueType: 'text',
      tooltip: 'Light Condition',
      valueText: () => clazz.state.environmentRequirement.lightCondition,
      validationState: () => EnvironmentRequirementComponent.validateString(clazz.state.environmentRequirement.lightCondition),
      onChange: (lightCondition) => clazz.setState({ environmentRequirement: { ...clazz.state.environmentRequirement, lightCondition } })
    }
    this.comments = {
      controlId: 'comments',
      labelText: 'Comments',
      placeHolderText: '',
      tooltip: 'Comments',
      valueType: 'text',
      valueText: () => clazz.state.environmentRequirement.comments,
      validationState: () => EnvironmentRequirementComponent.validateString(clazz.state.environmentRequirement.comments),
      onChange: (comments) => clazz.setState({ environmentRequirement: { ...clazz.state.environmentRequirement, comments } })
    }
  }

  render() {
    return (
        <div>
            <main>
                <Panel>
                    <Grid>
                        <Row styleClass="row-centered">
                            <Col md={3}>
                                <Form horizontal>
                                    <TextField {...this.temperature} />
                                </Form>
                            </Col>
                            <Col md={3}>
                                <Form horizontal>
                                    <TextField {...this.temperatureTolerance} />
                                </Form>
                            </Col>
                            <Col md={3}>
                                <Form horizontal>
                                    <TextField {...this.relativeHumidity} />
                                </Form>
                            </Col>
                            <Col md={3}>
                                <Form horizontal>
                                    <TextField {...this.relativeHumidityTolerance} />
                                </Form>
                            </Col>
                        </Row>
                        <Row styleClass="row-centered">
                            <Col md={3}>
                                <Form horizontal>
                                    <TextField {...this.inertAir} />
                                </Form>
                            </Col>
                            <Col md={3}>
                                <Form horizontal>
                                    <TextField {...this.inertAirTolerance} />
                                </Form>
                            </Col>
                            <Col md={6}>
                                <Form horizontal>
                                    <TextField {...this.renhold} />
                                </Form>
                            </Col>
                         </Row>
                         <Row styleClass="row-centered">
                            <Col md={6}>
                                <Form horizontal>
                                    <TextField {...this.lightCondition} />
                                </Form>
                            </Col>
                        </Row>
                        <Row styleClass="row-centered">
                            <Col md={12}>
                                <Form horizontal>
                                    <TextField {...this.comments} />
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
