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
        temperature: '',
        temperatureTolerance: '',
        relativeHumidity: '',
        relativeHumidityTolerance: '',
        inertAir: '',
        inertAirTolerance: '',
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
      id: 'temperature',
      tooltip: this.props.translate('musit.storageUnits.environmentRequirements.temperature.tooltip'),
      validate: 'text',
      placeHolder: this.props.translate('musit.storageUnits.environmentRequirements.temperature.placeHolder'),
      onChange: (temperature) => {
        this.setState({
          environmentRequirement: {
            ...this.state.environmentRequirement,
            temperature
          }
        })
      }
    }

    this.temperatureTolerance = {
      id: 'temperatureTolerance',
      tooltip: this.props.translate('musit.storageUnits.environmentRequirements.temperatureTolerance.tooltip'),
      validate: 'text',
      placeHolder: this.props.translate('musit.storageUnits.environmentRequirements.temperatureTolerance.placeHolder'),
      addOnPrefix: this.props.translate('musit.storageUnits.environmentRequirements.temperatureTolerance.addOnPrefix'),
      onChange: (temperatureTolerance) => {
        this.setState({
          environmentRequirement: {
            ...this.state.environmentRequirement,
            temperatureTolerance
          }
        })
      }
    }

    this.temperature = {
      id: 'temperature',
      tooltip: this.props.translate('musit.storageUnits.environmentRequirements.temperature.tooltip'),
      validate: 'text',
      placeHolder: this.props.translate('musit.storageUnits.environmentRequirements.temperature.placeHolder'),
      onChange: (temperature) => {
        this.setState({
          environmentRequirement: {
            ...this.state.environmentRequirement,
            temperature
          }
        })
      }
    }

    this.temperatureTolerance = {
      id: 'temperatureTolerance',
      tooltip: this.props.translate('musit.storageUnits.environmentRequirements.temperatureTolerance.tooltip'),
      validate: 'text',
      placeHolder: this.props.translate('musit.storageUnits.environmentRequirements.temperatureTolerance.placeHolder'),
      addOnPrefix: this.props.translate('musit.storageUnits.environmentRequirements.temperatureTolerance.addOnPrefix'),
      onChange: (temperatureTolerance) => {
        this.setState({
          environmentRequirement: {
            ...this.state.environmentRequirement,
            temperatureTolerance
          }
        })
      }
    }

    this.relativeHumidity = {
      id: 'relativeHumidity',
      tooltip: this.props.translate('musit.storageUnits.environmentRequirements.relativeHumidity.tooltip'),
      validate: 'text',
      placeHolder: this.props.translate('musit.storageUnits.environmentRequirements.relativeHumidity.placeHolder'),
      onChange: (relativeHumidity) => {
        this.setState({
          environmentRequirement: {
            ...this.state.environmentRequirement,
            relativeHumidity
          }
        })
      }
    }

    this.relativeHumidityTolerance = {
      id: 'relativeHumidityTolerance',
      tooltip: this.props.translate('musit.storageUnits.environmentRequirements.relativeHumidityTolerance.tooltip'),
      validate: 'text',
      placeHolder: this.props.translate('musit.storageUnits.environmentRequirements.relativeHumidityTolerance.placeHolder'),
      addOnPrefix: this.props.translate('musit.storageUnits.environmentRequirements.relativeHumidityTolerance.addOnPrefix'),
      onChange: (relativeHumidityTolerance) => {
        this.setState({
          environmentRequirement: {
            ...this.state.environmentRequirement,
            relativeHumidityTolerance
          }
        })
      }
    }

    this.inertAir = {
      id: 'inertAir',
      tooltip: this.props.translate('musit.storageUnits.environmentRequirements.inertAir.tooltip'),
      validate: 'text',
      placeHolder: this.props.translate('musit.storageUnits.environmentRequirements.inertAir.placeHolder'),
      onChange: (inertAir) => {
        this.setState({
          environmentRequirement: {
            ...this.state.environmentRequirement,
            inertAir
          }
        })
      }
    }

    this.inertAirTolerance = {
      id: 'inertAirTolerance',
      tooltip: this.props.translate('musit.storageUnits.environmentRequirements.inertAirTolerance.tooltip'),
      validate: 'text',
      placeHolder: this.props.translate('musit.storageUnits.environmentRequirements.inertAirTolerance.placeHolder'),
      addOnPrefix: this.props.translate('musit.storageUnits.environmentRequirements.inertAirTolerance.addOnPrefix'),
      onChange: (inertAirTolerance) => {
        this.setState({
          environmentRequirement: {
            ...this.state.environmentRequirement,
            inertAirTolerance
          }
        })
      }
    }

    this.renhold = {
      id: 'renhold',
      // placeHolder: 'test placeHolder',
      // addOnPrefix: '\u00b1',
      tooltip: this.props.translate('musit.storageUnits.environmentRequirements.renhold.tooltip'),
      validate: 'text',
      onChange: (renhold) => {
        this.setState({
          environmentRequirement: {
            ...this.state.environmentRequirement,
            renhold
          }
        })
      }
    }

    this.lightCondition = {
      id: 'lightCondition',
      tooltip: this.props.translate('musit.storageUnits.environmentRequirements.lightCondition.tooltip'),
      validate: 'text',
      onChange: (lightCondition) => {
        this.setState({
          environmentRequirement: {
            ...this.state.environmentRequirement,
            lightCondition
          }
        })
      }
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
  }

  render() {
    const renderFieldBlock = (bindValue, fieldProps, label) => (
      <FormGroup>
        <label className="col-sm-3 control-label" htmlFor="comments2">{label}</label>
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
                  <form className="form-horizontal">
                    <div className="form-group">
                      <label className="col-sm-3 control-label" htmlFor="comments2">{this.props.translate('musit.storageUnits.environmentRequirements.temperature.labelText')}</label>
                      <div class="col-sm-5" is="null">
                        <Field {...this.temperature} value={this.state.environmentRequirement.temperature} />
                      </div>
                      <div class="col-sm-4" is="null">
                        <Field {...this.temperatureTolerance} value={this.state.environmentRequirement.temperatureTolerance} />
                      </div>
                    </div>
                  </form>
                </Col>
                <Col md={6}>
                  <form className="form-horizontal">
                    <div className="form-group">
                      <label className="col-sm-3 control-label" htmlFor="comments2">{this.props.translate('musit.storageUnits.environmentRequirements.relativeHumidity.labelText')}</label>
                      <div class="col-sm-5" is="null">
                        <Field {...this.relativeHumidity} value={this.state.environmentRequirement.relativeHumidity} />
                      </div>
                      <div class="col-sm-4" is="null">
                        <Field {...this.relativeHumidityTolerance} value={this.state.environmentRequirement.relativeHumidityTolerance} />
                      </div>
                    </div>
                  </form>
                </Col>
              </Row>
              <Row styleClass="row-centered">
                <Col md={6}>
                  <form className="form-horizontal">
                    <div className="form-group">
                      <label className="col-sm-3 control-label" htmlFor="comments2">{this.props.translate('musit.storageUnits.environmentRequirements.inertAir.labelText')}</label>
                      <div class="col-sm-5" is="null">
                        <Field {...this.inertAir} value={this.state.environmentRequirement.inertAir} />
                      </div>
                      <div class="col-sm-4" is="null">
                        <Field {...this.inertAirTolerance} value={this.state.environmentRequirement.inertAirTolerance} />
                      </div>
                    </div>
                  </form>
                </Col>
                <Col md={6}>
                  <Form horizontal>
                    {renderFieldBlock(this.state.environmentRequirement.renhold, this.renhold,
                      this.props.translate('musit.storageUnits.environmentRequirements.renhold.labelText'))}
                  </Form>
                </Col>
              </Row>
              <Row styleClass="row-centered">
                <Col md={6}>
                  <Form horizontal>
                    {renderFieldBlock(this.state.environmentRequirement.lightCondition, this.lightCondition,
                      this.props.translate('musit.storageUnits.environmentRequirements.lightCondition.labelText'))}
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
