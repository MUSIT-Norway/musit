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
        airRatio: 'airRatio',
        renhold: 'renhold',
        comments: 'comments here'
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

    this.airRatio = {
      controlId: 'airRatio',
      labelText: 'Air Ratio',
      placeHolderText: 'enter description here',
      valueType: 'text',
      tooltip: 'Air Ratio',
      valueText: () => clazz.state.environmentRequirement.airRatio,
      validationState: () => EnvironmentRequirementComponent.validateString(clazz.state.environmentRequirement.airRatio),
      onChange: (airRatio) => clazz.setState({ environmentRequirement: { ...clazz.state.environmentRequirement, airRatio } })
    }
    this.renhold = {
      controlId: 'renhold',
      labelText: 'Renhold',
      placeHolderText: 'enter description here',
      tooltip: 'Renhold',
      valueType: 'text',
      valueText: () => clazz.state.environmentRequirement.renhold,
      validationState: () => EnvironmentRequirementComponent.validateString(clazz.state.environmentRequirement.renhold),
      onChange: (renhold) => clazz.setState({ environmentRequirement: { ...clazz.state.environmentRequirement, renhold } })
    }
    this.comments = {
      controlId: 'comments',
      labelText: 'Comments',
      placeHolderText: 'enter description here',
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
                            <Col md={6}>
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
                                    </Row>
                                    <Row styleClass="row-centered">
                                        <Col md={6}>
                                            <Form horizontal>
                                                <TextField {...this.relativeHumidity} />
                                            </Form>
                                        </Col>
                                    </Row>
                                    <Row styleClass="row-centered">
                                        <Col md={6}>
                                            <Form horizontal>
                                                <TextField {...this.inertAir} />
                                            </Form>
                                        </Col>
                                    </Row>
                                </Grid>
                            </Col>
                            <Col md={6}>
                                <Grid>
                                    <Row styleClass="row-centered">
                                        <Col md={6}>
                                            <Form horizontal>
                                                <TextField {...this.airRatio} />
                                            </Form>
                                        </Col>
                                    </Row>
                                    <Row styleClass="row-centered">
                                        <Col md={6}>
                                            <Form horizontal>
                                                <TextField {...this.renhold} />
                                            </Form>
                                        </Col>
                                    </Row>

                                </Grid>
                            </Col>
                        </Row>
                        <Row styleClass="row-centered">
                            <Col md={16}>
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
