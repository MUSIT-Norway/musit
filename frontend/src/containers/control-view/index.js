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
import { Grid, Row, Col, ButtonToolbar, Button } from 'react-bootstrap'
import PairedToogleButtons from '../../components/controls/pairedToggleButtons'
import Field from '../../components/formfields/musitfield'
import { addControl } from '../../reducers/control'
import Language from '../../components/language'

const mapStateToProps = (state) => ({
  user: state.auth.user,
  environmentRequirements: state.environmentRequirements,
  translate: (key, markdown) => Language.translate(key, markdown),
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
      relativeHumidity: null,
      pestOK: null,
      storageUnit: null
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
    if (this.state.relativeHumidity != null && this.state.relativeHumidity) {
      this.setState({ ...this.state, relativeHumidity: null })
    } else {
      this.setState({ ...this.state, relativeHumidity: true })
    }
  }

  onRelativeHumidityNotOKClick() {
    if (this.state.relativeHumidity != null && !this.state.relativeHumidity) {
      this.setState({ ...this.state, relativeHumidity: null })
    } else {
      this.setState({ ...this.state, relativeHumidity: false })
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


  getDate() {
    const d = new Date()
    const r = `${d.getDate()}/${d.getMonth()}/${d.getFullYear()}`
    return r
  }

  render() {
    return (
      <div>
        <Grid>
          <Row>
            <h1 />
          </Row>
          <Row>
            <Col md={3}>
              {this.getDate()}
            </Col>
            <Col md={9}>
              {this.props.user ? this.props.user.name : null}
            </Col>
          </Row>
          <Row styleclass="row-centered">
            <Col md={3}>
              <PairedToogleButtons
                label="Temperatur"
                value={this.state.temperatureOK}
                updatevalueOK={this.onTemperatureOKClick}
                updatevalueNotOK={this.onTemperatureNotOKClick}
              />
            </Col>
            <Col md={9}>
              <label>Hei</label>
              <Field value="Hei" />

            </Col>
          </Row>
          <Row>
            <Col md={3}>
              <PairedToogleButtons
                label="Inert luft"
                value={this.state.inertAirOK}
                updatevalueOK={this.onInertAirOKClick}
                updatevalueNotOK={this.onInertAirNotOKClick}
              />
            </Col>
            <Col md={9} />
          </Row>
          <Row>
            <Col md={3}>
              <PairedToogleButtons
                label="Relativ luftfuktighet"
                value={this.state.relativeHumidity}
                updatevalueOK={this.onRelativeHumidityOKClick}
                updatevalueNotOK={this.onRelativeHumidityNotOKClick}
              />
            </Col>
            <Col md={9} />
          </Row>
          <Row>
            <Col md={3}>
              <PairedToogleButtons
                label="Rengjøring"
                value={this.state.cleaningOK}
                updatevalueOK={this.onCleaningOKClick}
                updatevalueNotOK={this.onCleaningNotOKClick}
              />
            </Col>
            <Col md={9} />
          </Row>
          <Row>
            <Col md={3}>
              <PairedToogleButtons
                label="Lysforhold"
                value={this.state.lightConditionsOK}
                updatevalueOK={this.onLightConditionsOKClick}
                updatevalueNotOK={this.onLightConditionsNotOKClick}
              />
            </Col>
            <Col md={9} />
          </Row>
          <Row>
            <Col md={3}>
              <PairedToogleButtons
                label="Sprit"
                value={this.state.alchoholOK}
                updatevalueOK={this.onAlchoholOKClick}
                updatevalueNotOK={this.onAlchoholNotOKClick}
              />
            </Col>
            <Col md={9} />
          </Row>
          <Row>
            <Col md={3}>
              <PairedToogleButtons
                label="Skadedyr"
                value={this.state.pestOK}
                updatevalueOK={this.onPestOKClick}
                updatevalueNotOK={this.onPestNotOKClick}
              />
            </Col>
            <Col md={9} />
          </Row>
          <Row>
            <Col md={3}>
              <PairedToogleButtons
                label="Mugg"
                value={this.state.moldFungusOK}
                updatevalueOK={this.onMoldFungusOKClick}
                updatevalueNotOK={this.onMoldFungusNotOKClick}
              />
            </Col>
            <Col md={9} />
          </Row>
          <Row>
            <Col md={3} />
            <Col md={9}>
              <Button pullRight onClick={() => this.props.onLagreControl(this.state)}> Lagre </Button>
            </Col>
          </Row>
        </Grid>
      </div>)
  }
}
