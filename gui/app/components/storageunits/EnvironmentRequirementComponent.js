/**
 * Created by steinaol on 5/27/16.
 */

import React, { Component } from 'react';
import { MusitTextArea as TextArea, MusitField as Field, MusitTextField as TextField } from '../../components/formfields'
import { Panel, Form, Grid, Row, Col, FormGroup } from 'react-bootstrap'

export default class EnvironmentRequirementComponent extends Component {
  static propTypes = {
    translate: React.PropTypes.func.isRequired,
    user: React.PropTypes.object,
  };

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
        comments: '',
        comments2: ''
      }
    }

    const clazz = this

    this.temperature = {
      controlId: 'temperature1',
      controlId2: 'temperature2',
      valueType: 'text',
      labelText: this.props.translate('musit.storageUnits.environmentRequirements.temperature.labelText'),
      labelText2: '\u00b1',
      tooltip: this.props.translate('musit.storageUnits.environmentRequirements.temperature.tooltip'),
      placeHolderText: this.props.translate('musit.storageUnits.environmentRequirements.temperature.placeHolderText'),
      placeHolderText2: this.props.translate('musit.storageUnits.environmentRequirements.temperature.placeHolderText2'),
      valueText: () => clazz.state.environmentRequirement.temperature1,
      valueText2: () => clazz.state.environmentRequirement.temperature2,
      validationState: () => EnvironmentRequirementComponent.validateNumber(clazz.state.environmentRequirement.temperature1),
      validationState2: () => EnvironmentRequirementComponent.validateNumber(clazz.state.environmentRequirement.temperature2),
      onChange: (temperature1) => clazz.setState({
        environmentRequirement: { ...this.state.environmentRequirement, temperature1 }
      }),
      onChange2: (temperature2) => clazz.setState({
        environmentRequirement: { ...this.state.environmentRequirement, temperature2 }
      })
    }

    this.relativeHumidity = {
      controlId: 'relativeHumidity1',
      controlId2: 'relativeHumidity2',
      valueType: 'text',
      labelText: this.props.translate('musit.storageUnits.environmentRequirements.relativeHumidity.labelText'),
      labelText2: '\u00b1',
      tooltip: this.props.translate('musit.storageUnits.environmentRequirements.relativeHumidity.tooltip'),
      placeHolderText: this.props.translate('musit.storageUnits.environmentRequirements.relativeHumidity.placeHolderText'),
      placeHolderText2: this.props.translate('musit.storageUnits.environmentRequirements.relativeHumidity.placeHolderText2'),
      valueText: () => clazz.state.environmentRequirement.relativeHumidity1,
      valueText2: () => clazz.state.environmentRequirement.relativeHumidity2,
      validationState: () => EnvironmentRequirementComponent.validateNumber(clazz.state.environmentRequirement.relativeHumidity1),
      validationState2: () =>
        EnvironmentRequirementComponent.validateNumber(clazz.state.environmentRequirement.relativeHumidity2),
      onChange: (relativeHumidity1) => clazz.setState({
        environmentRequirement: { ...this.state.environmentRequirement, relativeHumidity1 }
      }),
      onChange2: (relativeHumidity2) => clazz.setState({
        environmentRequirement: { ...this.state.environmentRequirement, relativeHumidity2 }
      })
    }

    this.inertAir = {
      controlId: 'inertAir1',
      controlId2: 'inertAir2',
      valueType: 'text',
      labelText: this.props.translate('musit.storageUnits.environmentRequirements.inertAir.labelText'),
      labelText2: '\u00b1',
      tooltip: this.props.translate('musit.storageUnits.environmentRequirements.inertAir.tooltip'),
      placeHolderText: this.props.translate('musit.storageUnits.environmentRequirements.inertAir.placeHolderText'),
      placeHolderText2: this.props.translate('musit.storageUnits.environmentRequirements.inertAir.placeHolderText2'),
      valueText: () => clazz.state.environmentRequirement.inertAir1,
      valueText2: () => clazz.state.environmentRequirement.inertAir2,
      validationState: () => EnvironmentRequirementComponent.validateNumber(clazz.state.environmentRequirement.inertAir1),
      validationState2: () => EnvironmentRequirementComponent.validateNumber(clazz.state.environmentRequirement.inertAir2),
      onChange: (inertAir1) => clazz.setState({
        environmentRequirement: {
          ...this.state.environmentRequirement,
          inertAir1
        }
      }),
      onChange2: (inertAir2) => clazz.setState({
        environmentRequirement: {
          ...this.state.environmentRequirement,
          inertAir2
        }
      })
    }

    this.renhold = {
      controlId: 'renhold',
      valueType: 'text',
      labelText: this.props.translate('musit.storageUnits.environmentRequirements.renhold.labelText'),
      tooltip: this.props.translate('musit.storageUnits.environmentRequirements.renhold.tooltip'),
      // placeHolderText: this.props.translate('musit.storageUnits.environmentRequirements.renhold.placeHolderText'),
      valueText: () => clazz.state.environmentRequirement.renhold,
      validationState: () => EnvironmentRequirementComponent.validateString(clazz.state.environmentRequirement.renhold),
      onChange: (renhold) => clazz.setState({
        environmentRequirement: {
          ...clazz.state.environmentRequirement,
          renhold
        }
      })
    }

    this.lightCondition = {
      controlId: 'lightCondition',
      valueType: 'text',
      labelText: this.props.translate('musit.storageUnits.environmentRequirements.lightCondition.labelText'),
      tooltip: this.props.translate('musit.storageUnits.environmentRequirements.lightCondition.tooltip'),
      // placeHolderText: this.props.translate('musit.storageUnits.environmentRequirements.lightCondition.placeHolderText'),
      valueText: () => clazz.state.environmentRequirement.lightCondition,
      validationState: () => EnvironmentRequirementComponent.validateString(clazz.state.environmentRequirement.lightCondition),
      onChange: (lightCondition) => clazz.setState({
        environmentRequirement: { ...clazz.state.environmentRequirement, lightCondition }
      })
    }

    this.comments = {
      controlId: 'comments',
      labelText: 'Kommentar',
      placeHolderText: '',
      tooltip: 'Kommentar',
      valueText: () => clazz.state.environmentRequirement.comments,
      validationState: () => EnvironmentRequirementComponent.validateString(clazz.state.environmentRequirement.comments),
      onChange: (comments) => clazz.setState({
        environmentRequirement: {
          ...clazz.state.environmentRequirement,
          comments
        }
      })
    }

    this.comments2 = {
      id: 'comments2',
      placeHolder: 'test placeHolder',
      tooltip: 'Kommentar',
      // value: this.state.environmentRequirement.comments2,
      help: 'Help text',
      addOnPrefix: '\u00b1',
      // validate: () => EnvironmentRequirementComponent.validateString(this.state.environmentRequirement.comments2),
      validate: 'text',
      minimumLength: 2,
      maximumLength: 5,
      /* validator: (value, {}) => {
        console.log('custom validator')
        const isSomething = value === 'test'
        return isSomething ? 'success' : 'error'
      },*/
      onChange: (comments2) => {
        this.setState({
          environmentRequirement: {
            ...this.state.environmentRequirement,
            comments2
          }
        })
      }
    }
  }

  render() {
    const renderFieldBlock = (bindValue, fieldProps) => (
      <FormGroup>
        <label className="col-sm-3 control-label" htmlFor="comments2">Kommentar</label>
        <div class="col-sm-9" is="null">
          <Field {...fieldProps} value={bindValue} />
        </div>
      </FormGroup>
    )

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
              <Row>
                <Col md={6}>
                  <Form horizontal>
                    {renderFieldBlock(this.state.environmentRequirement.comments2, this.comments2)}
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
