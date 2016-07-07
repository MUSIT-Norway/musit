/*
 *  MUSIT is a museum database to archive natural and cultural history data.
 *  Copyright (C) 2016  MUSIT Norway, part of www.uio.no (University of Oslo)
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License,
 *  or any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

import React from 'react'
import { connect } from 'react-redux'
import { Grid, Row, Col, Button, FormControl, PageHeader } from 'react-bootstrap'
import PairedToogleButtons from '../../../components/control/add'
import Field from '../../../components/formfields/musitfield'
import { addControl } from '../../../reducers/control/add'
import Language from '../../../components/language'
import DatePicker from 'react-bootstrap-date-picker'
import moment from 'moment'

const mapStateToProps = (state) => ({
  user: state.auth.user,
  environmentRequirements: state.environmentRequirements,
  translate: (key, markdown) => Language.translate(key, markdown)
})

const mapDispatchToProps = (dispatch) => ({
  onLagreControl: (data) => {
    dispatch(addControl(data))
  },
  updateControl: (data) => {
    dispatch(addControl(data))
  }
})

@connect(mapStateToProps, mapDispatchToProps)

export default class ControlView extends React.Component {
  static propTypes = {
    translate: React.PropTypes.func.isRequired,
    user: React.PropTypes.string.isRequired,
    onLagreControl: React.PropTypes.func.isRequired,
    updateControl: React.PropTypes.func.isRequired,
  }
  constructor(props) {
    super(props)
    this.state = {
      temperatureOK: null,
      inertAirOK: null,
      gasOK: null,
      lightConditionsOK: null,
      cleaningOK: null,
      alchoholOK: null,
      moldFungusOK: null,
      relativeHumidityOK: null,
      pestOK: null,
      storageUnit: null,
      temperature: '12',
      temperatureTolerance: '2',
      relativeHumidity: '89',
      relativeHumidityInterval: '4',
      inertAir: '56',
      inertAirInterval: '4',
      light: 'Mørkt',
      cleaning: 'Gullende rent',
      startDate: moment(),
      user: this.props.user ? this.props.user.name : ''
    }
    this.getDate = this.getDate.bind(this)
    this.onTemperatureOKClick = this.onTemperatureOKClick.bind(this)
    this.onTemperatureNotOKClick = this.onTemperatureNotOKClick.bind(this)
    this.onInertAirOKClick = this.onInertAirOKClick.bind(this)
    this.onInertAirNotOKClick = this.onInertAirNotOKClick.bind(this)
    this.onRelativeHumidityOKClick = this.onRelativeHumidityOKClick.bind(this)
    this.onRelativeHumidityNotOKClick = this.onRelativeHumidityNotOKClick.bind(this)

    this.onCleaningOKClick = this.onCleaningOKClick.bind(this)
    this.onCleaningNotOKClick = this.onCleaningNotOKClick.bind(this)

    this.onLightConditionsOKClick = this.onLightConditionsOKClick.bind(this)
    this.onLightConditionsNotOKClick = this.onLightConditionsNotOKClick.bind(this)

    this.onAlchoholOKClick = this.onAlchoholOKClick.bind(this)
    this.onAlchoholNotOKClick = this.onAlchoholNotOKClick.bind(this)

    this.onPestOKClick = this.onPestOKClick.bind(this)
    this.onPestNotOKClick = this.onPestNotOKClick.bind(this)

    this.onMoldFungusOKClick = this.onMoldFungusOKClick.bind(this)
    this.onMoldFungusNotOKClick = this.onMoldFungusNotOKClick.bind(this)

    this.onGasOKClick = this.onGasOKClick.bind(this)
    this.onGasNotOKClick = this.onGasNotOKClick.bind(this)
    this.onHandleDateChange = this.onHandleDateChange.bind(this)
  }

  onHandleDateChange(d) {
    this.setState({ ...this.state, startDate: d })
  }

  onTemperatureOKClick() {
    if (this.state.temperatureOK != null && this.state.temperatureOK) {
      this.setState({ ...this.state, temperatureOK: null })
    } else {
      this.setState({ ...this.state, temperatureOK: true })
    }
    this.props.updateControl(this.state)
  }

  onTemperatureNotOKClick() {
    if (this.state.temperatureOK != null && !this.state.temperatureOK) {
      this.setState({ ...this.state, temperatureOK: null })
    } else {
      this.setState({ ...this.state, temperatureOK: false })
    }
    this.props.updateControl(this.state)
  }

  onInertAirOKClick() {
    if (this.state.inertAirOK != null && this.state.inertAirOK) {
      this.setState({ ...this.state, inertAirOK: null })
    } else {
      this.setState({ ...this.state, inertAirOK: true })
    }
  }

  onInertAirNotOKClick() {
    if (this.state.inertAirOK != null && !this.state.inertAirOK) {
      this.setState({ ...this.state, inertAirOK: null })
    } else {
      this.setState({ ...this.state, inertAirOK: false })
    }
  }

  onRelativeHumidityOKClick() {
    if (this.state.relativeHumidityOK != null && this.state.relativeHumidityOK) {
      this.setState({ ...this.state, relativeHumidityOK: null })
    } else {
      this.setState({ ...this.state, relativeHumidityOK: true })
    }
  }

  onRelativeHumidityNotOKClick() {
    if (this.state.relativeHumidityOK != null && !this.state.relativeHumidityOK) {
      this.setState({ ...this.state, relativeHumidityOK: null })
    } else {
      this.setState({ ...this.state, relativeHumidityOK: false })
    }
  }

  onCleaningOKClick() {
    if (this.state.cleaningOK != null && this.state.cleaningOK) {
      this.setState({ ...this.state, cleaningOK: null })
    } else {
      this.setState({ ...this.state, cleaningOK: true })
    }
  }
  onCleaningNotOKClick() {
    if (this.state.cleaningOK != null && !this.state.cleaningOK) {
      this.setState({ ...this.state, cleaningOK: null })
    } else {
      this.setState({ ...this.state, cleaningOK: false })
    }
  }

  onLightConditionsOKClick() {
    if (this.state.lightConditionsOK != null && this.state.lightConditionsOK) {
      this.setState({ ...this.state, lightConditionsOK: null })
    } else {
      this.setState({ ...this.state, lightConditionsOK: true })
    }
  }
  onLightConditionsNotOKClick() {
    if (this.state.lightConditionsOK != null && !this.state.lightConditionsOK) {
      this.setState({ ...this.state, lightConditionsOK: null })
    } else {
      this.setState({ ...this.state, lightConditionsOK: false })
    }
  }

  onAlchoholOKClick() {
    if (this.state.alchoholOK != null && this.state.alchoholOK) {
      this.setState({ ...this.state, alchoholOK: null })
    } else {
      this.setState({ ...this.state, alchoholOK: true })
    }
  }
  onAlchoholNotOKClick() {
    if (this.state.alchoholOK != null && !this.state.alchoholOK) {
      this.setState({ ...this.state, alchoholOK: null })
    } else {
      this.setState({ ...this.state, alchoholOK: false })
    }
  }

  onPestOKClick() {
    if (this.state.pestOK != null && this.state.pestOK) {
      this.setState({ ...this.state, pestOK: null })
    } else {
      this.setState({ ...this.state, pestOK: true })
    }
  }
  onPestNotOKClick() {
    if (this.state.pestOK != null && !this.state.pestOK) {
      this.setState({ ...this.state, pestOK: null })
    } else {
      this.setState({ ...this.state, pestOK: false })
    }
  }

  onMoldFungusOKClick() {
    if (this.state.moldFungusOK != null && this.state.moldFungusOK) {
      this.setState({ ...this.state, moldFungusOK: null })
    } else {
      this.setState({ ...this.state, moldFungusOK: true })
    }
  }
  onMoldFungusNotOKClick() {
    if (this.state.moldFungusOK != null && !this.state.moldFungusOK) {
      this.setState({ ...this.state, moldFungusOK: null })
    } else {
      this.setState({ ...this.state, moldFungusOK: false })
    }
  }

  onGasOKClick() {
    if (this.state.gasOK != null && this.state.gasOK) {
      this.setState({ ...this.state, gasOK: null })
    } else {
      this.setState({ ...this.state, gasOK: true })
    }
  }


  onGasNotOKClick() {
    if (this.state.gasOK != null && !this.state.gasOK) {
      this.setState({ ...this.state, gasOK: null })
    } else {
      this.setState({ ...this.state, gasOK: false })
    }
  }


  getDate() {
    // const r = `${d.getDate()}/${d.getMonth()}/${d.getFullYear()}`
    return moment().format('mm/dd/yyyy');
  }


  render() {
    const stateOKorNotOK = () => {
      return (this.state.temperatureOK != null ||
        this.state.relativeHumidityOK != null ||
        this.state.inertAirOK != null ||
        this.state.lightConditionsOK != null ||
        this.state.cleaningOK != null ||
        this.state.gasOK != null ||
        this.state.alchoholOK != null ||
        this.state.moldFungusOK != null ||
        this.state.pestOK != null)
    }

    const { translate } = this.props
    const renderReadOnly = (v) => {
      return <FormControl style={{ backgroundColor: '#f2f2f2' }} readonly value={v} />
    }
    return (
      <div>
        <Grid>
          <Row>
            <h1 />
          </Row>
          <Row>
            <h1 />
          </Row>
          <Row>
            <h1 />
          </Row>
          <Row>
            <h1 />
          </Row>
          <Row styleClass="row-centered">
            <PageHeader>
              {this.props.translate('musit.newControl.title', true)}
            </PageHeader>
          </Row>

          <Row>
            <Col md={3}>
              <Col md={2} />
              <Col md={10}>
                <Row>
                  <Col md={12}>
                    <label>
                        {translate('musit.newControl.date')}
                    </label>
                  </Col>
                </Row>
                <Row>
                  <Col md={12}>
                    <DatePicker
                      dateFormat="DD/MM/YYYY"
                      value={this.state.startDate}
                      onChange={this.onHandleDateChange}
                    />
                  </Col>
                </Row>
              </Col>
            </Col>
            <Col md={9}>
              <Row>
                <Col md={5}>
                  <label>
                    {translate('musit.newControl.doneBy')}
                  </label>
                </Col>
              </Row>
              <Row>
                <Col md={9}>
                  <Field
                    value={this.state.user}
                    onChange={(v) => { this.setState({ ...this.state, user: v }) }}
                  />
                </Col>
              </Row>
            </Col>
          </Row>

          <Row>
            <Col md={12}>
              <hr />
            </Col>
          </Row>
          <Row>
            <h1 />
          </Row>
          <Row>
            <Col md={3}>
              <PairedToogleButtons
                label={translate('musit.newControl.temperature')}
                value={this.state.temperatureOK}
                updatevalueOK={this.onTemperatureOKClick}
                updatevalueNotOK={this.onTemperatureNotOKClick}
              />
            </Col>
            <Col md={9}>
              <Row>
                <Col md={5}>
                  <label> {translate('musit.newControl.envdata')} </label>
                </Col>
              </Row>
              <Row>
                <Col md={5}>
                  {renderReadOnly(this.state.temperature)}
                </Col>
                <Col md={4}>
                  {renderReadOnly(this.state.temperatureTolerance)}
                </Col>
              </Row>
            </Col>
          </Row>

          <Row>
            <Col md={12}>
              <hr />
            </Col>
          </Row>

          <Row>
            <Col md={3}>
              <PairedToogleButtons
                label={translate('musit.newControl.relativeHumidity')}
                value={this.state.relativeHumidityOK}
                updatevalueOK={this.onRelativeHumidityOKClick}
                updatevalueNotOK={this.onRelativeHumidityNotOKClick}
              />
            </Col>
            <Col md={9}>
              <Row>
                <Col md={5}>
                  <label> {translate('musit.newControl.envdata')} </label>
                </Col>
              </Row>
              <Row>
                <Col md={5}>
                  {renderReadOnly(this.state.relativeHumidity)}
                </Col>
                <Col md={4}>
                  {renderReadOnly(this.state.relativeHumidityInterval)}
                </Col>
              </Row>
            </Col>
          </Row>

          <Row>
            <Col md={12}>
              <hr />
            </Col>
          </Row>

          <Row>
            <Col md={3}>
              <PairedToogleButtons
                label={translate('musit.newControl.inertAir')}
                value={this.state.inertAirOK}
                updatevalueOK={this.onInertAirOKClick}
                updatevalueNotOK={this.onInertAirNotOKClick}
              />
            </Col>
            <Col md={9}>
              <Row>
                <Col md={5}>
                  <label> {translate('musit.newControl.envdata')} </label>
                </Col>
              </Row>
              <Row>
                <Col md={5}>
                  {renderReadOnly(this.state.inertAir)}
                </Col>
                <Col md={4}>
                  {renderReadOnly(this.state.inertAirInterval)}
                </Col>
              </Row>
            </Col>
          </Row>

          <Row>
            <Col md={12}>
              <hr />
            </Col>
          </Row>


          <Row>
            <Col md={3}>
              <PairedToogleButtons
                label={translate('musit.newControl.lightCondition')}
                value={this.state.lightConditionsOK}
                updatevalueOK={this.onLightConditionsOKClick}
                updatevalueNotOK={this.onLightConditionsNotOKClick}
              />
            </Col>
            <Col md={9}>
              <Row>
                <Col md={5}>
                  <label> {translate('musit.newControl.envdata')} </label>
                </Col>
              </Row>
              <Row>
                <Col md={9}>
                  {renderReadOnly(this.state.light)}
                </Col>
              </Row>
            </Col>
          </Row>

          <Row>
            <Col md={12}>
              <hr />
            </Col>
          </Row>
          <Row>
            <Col md={3}>
              <PairedToogleButtons
                label={translate('musit.newControl.cleaning')}
                value={this.state.cleaningOK}
                updatevalueOK={this.onCleaningOKClick}
                updatevalueNotOK={this.onCleaningNotOKClick}
              />
            </Col>
            <Col md={9}>
              <Row>
                <Col md={5}>
                  <label> {translate('musit.newControl.envdata')} </label>
                </Col>
              </Row>
              <Row>
                <Col md={9}>
                  {renderReadOnly(this.state.cleaning)}
                </Col>
              </Row>
            </Col>
          </Row>

          <Row>
            <Col md={12}>
              <hr />
            </Col>
          </Row>

          <Row>
            <Col md={3}>
              <PairedToogleButtons
                label={translate('musit.newControl.gas')}
                value={this.state.gasOK}
                updatevalueOK={this.onGasOKClick}
                updatevalueNotOK={this.onGasNotOKClick}
              />
            </Col>
            <Col md={9}>
              <Row>
                <Col md={5}>
                  <label> {translate('musit.newControl.envdata')} </label>
                </Col>
              </Row>
              <Row>
                <Col md={9}> --- </Col>
              </Row>
            </Col>
          </Row>

          <Row>
            <Col md={12}>
              <hr />
            </Col>
          </Row>


          <Row>
            <Col md={3}>
              <PairedToogleButtons
                label={translate('musit.newControl.alchohol')}
                value={this.state.alchoholOK}
                updatevalueOK={this.onAlchoholOKClick}
                updatevalueNotOK={this.onAlchoholNotOKClick}
              />
            </Col>
            <Col md={9}>
              <Row>
                <Col md={5}>
                  <label> {translate('musit.newControl.envdata')} </label>
                </Col>
              </Row>
              <Row>
                <Col md={9}>
                  ---
                </Col>
              </Row>
            </Col>
          </Row>


          <Row>
            <Col md={12}>
              <hr />
            </Col>
          </Row>


          <Row>
            <Col md={3}>
              <PairedToogleButtons
                label={translate('musit.newControl.mold')}
                value={this.state.moldFungusOK}
                updatevalueOK={this.onMoldFungusOKClick}
                updatevalueNotOK={this.onMoldFungusNotOKClick}
              />
            </Col>
            <Col md={9}>
              <Row>
                <Col md={5}>
                  <label> {translate('musit.newControl.envdata')} </label>
                </Col>
              </Row>
              <Row>
                <Col md={9}> --- </Col>
              </Row>
            </Col>
          </Row>

          <Row>
            <Col md={12}>
              <hr />
            </Col>
          </Row>


          <Row>
            <Col md={3}>
              <PairedToogleButtons
                label={translate('musit.newControl.pest')}
                value={this.state.pestOK}
                updatevalueOK={this.onPestOKClick}
                updatevalueNotOK={this.onPestNotOKClick}
              />
            </Col>
            <Col md={9}>
              <Row>
                <Col md={5}>
                  <label> {translate('musit.newControl.envdata')} </label>
                </Col>
              </Row>
              <Row>
                <Col md={9}>
                  ---
                </Col>
              </Row>
            </Col>
          </Row>

          <Row>
            <Col md={12}>
              <p>
                <hr />
              </p>
            </Col>
          </Row>


          <Row>
            <Col md={3} />
            <Col md={9} mdPush={8}>
              <Button
                disabled={!stateOKorNotOK()}
                text-align="right"
                pullRight onClick={() =>
                this.props.onLagreControl(this.state)}
              >
                Registrer observasjon
              </Button>
            </Col>
          </Row>
        </Grid>
      </div>)
  }
}
