/**
 * Created by steinaol on 5/27/16.
 */

import React, { Component } from 'react';
import TextField from '../../components/musittextfield';
import TextArea from '../../components/musittextarea';
import { Panel, Form, Grid, Row, Col } from 'react-bootstrap'

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
        temperature1: '',
        temperature2: '',
        relativeHumidity1: '',
        relativeHumidity2: '',
        inertAir1: '',
        inertAir2: '',
        renhold: '',
        lightCondition: '',
        comments: ''
      }
    }

    const clazz = this

    this.temperature = {
      controlId: 'temperature1',
      controlId2: 'temperature2',
      labelText: 'Temperature',
      tooltip: 'Temperature',
      valueType: 'number',
      placeHolderText: 'C',
      placeHolderText2: 'int',
      valueText: () => clazz.state.environmentRequirement.temperature1,
      valueText2: () => clazz.state.environmentRequirement.temperature2,
      validationState: () => EnvironmentRequirementComponent.validateNumber(clazz.state.environmentRequirement.temperature1),
      validationState2: () => EnvironmentRequirementComponent.validateNumber(clazz.state.environmentRequirement.temperature2),
      onChange: (temperature1) => clazz.setState({ environmentRequirement:
      { ...this.state.environmentRequirement, temperature1 } }),
      onChange2: (temperature2) => clazz.setState({ environmentRequirement:
      { ...this.state.environmentRequirement, temperature2 } })
    }

    this.relativeHumidity = {
      controlId: 'relativeHumidity1',
      controlId2: 'relativeHumidity2',
      labelText: 'Rel. luftfuktighet',
      tooltip: 'Rel. luftfuktighet',
      valueType: 'number',
      placeHolderText: '%',
      placeHolderText2: 'int',
      valueText: () => clazz.state.environmentRequirement.relativeHumidity1,
      valueText2: () => clazz.state.environmentRequirement.relativeHumidity2,
      validationState: () => EnvironmentRequirementComponent.validateNumber(clazz.state.environmentRequirement.relativeHumidity1),
      validationState2: () =>
      EnvironmentRequirementComponent.validateNumber(clazz.state.environmentRequirement.relativeHumidity2),
      onChange: (relativeHumidity1) => clazz.setState({ environmentRequirement:
      { ...this.state.environmentRequirement, relativeHumidity1 } }),
      onChange2: (relativeHumidity2) => clazz.setState({ environmentRequirement:
      { ...this.state.environmentRequirement, relativeHumidity2 } })
    }

    this.inertAir = {
      controlId: 'inertAir1',
      controlId2: 'inertAir2',
      labelText: 'Inertluft',
      tooltip: 'Inertluft',
      valueType: 'number',
      placeHolderText: '% O2',
      placeHolderText2: 'int',
      valueText: () => clazz.state.environmentRequirement.inertAir1,
      valueText2: () => clazz.state.environmentRequirement.temperature2,
      validationState: () => EnvironmentRequirementComponent.validateNumber(clazz.state.environmentRequirement.inertAir1),
      validationState2: () => EnvironmentRequirementComponent.validateNumber(clazz.state.environmentRequirement.inertAir2),
      onChange: (inertAir1) => clazz.setState({ environmentRequirement: { ...this.state.environmentRequirement, inertAir1 } }),
      onChange2: (inertAir2) => clazz.setState({ environmentRequirement: { ...this.state.environmentRequirement, inertAir2 } })
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
      labelText: 'Lysforhold',
      placeHolderText: '',
      valueType: 'text',
      tooltip: 'Lysforhold',
      valueText: () => clazz.state.environmentRequirement.lightCondition,
      validationState: () => EnvironmentRequirementComponent.validateString(clazz.state.environmentRequirement.lightCondition),
      onChange: (lightCondition) => clazz.setState({ environmentRequirement:
      { ...clazz.state.environmentRequirement, lightCondition } })
    }
    this.comments = {
      controlId: 'comments',
      labelText: 'Kommentar',
      placeHolderText: '',
      tooltip: 'Kommentar',
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
                                <Form horizontal>
                                    <TextField {...this.temperature} />
                                </Form>
                            </Col>
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
                        <Row>
                            <Col md={6}>
                                <Form horizontal>
                                    <TextArea{...this.comments} />
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
