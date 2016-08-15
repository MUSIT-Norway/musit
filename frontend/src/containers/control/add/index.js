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
import { Grid, Row, Col, FormControl, PageHeader } from 'react-bootstrap'
import PairedToogleButtons from '../../../components/control/add'
import Field from '../../../components/formfields/musitfield'
import { addControl } from '../../../reducers/control/add'
import Language from '../../../components/language'
import DatePicker from 'react-bootstrap-date-picker'
import moment from 'moment'
import SaveCancel from '../../../components/formfields/saveCancel/SaveCancel'
import { hashHistory } from 'react-router'
import { flatten } from '../../../util'

const mapStateToProps = (state) => ({
  user: state.auth.user,
  translate: (key, markdown) => Language.translate(key, markdown)
})

const mapDispatchToProps = (dispatch) => ({
  saveControl: (data) => {
    dispatch(addControl(data))
  }
})

@connect(mapStateToProps, mapDispatchToProps)
export default class ControlView extends React.Component {
  static propTypes = {
    translate: React.PropTypes.func.isRequired,
    user: React.PropTypes.object,
    saveControl: React.PropTypes.func.isRequired
  }

  constructor(props) {
    super(props)
    this.state = {
      temperatureOK: null,
      inertAirOK: null,
      gasOK: null,
      lightConditionsOK: null,
      cleaningOK: null,
      alcoholOK: null,
      moldOK: null,
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
      startDate: moment().format(),
      user: this.props.user ? this.props.user.name : ''
    }
    this.getDate = this.getDate.bind(this)

    this.onControlClick = this.onControlClick.bind(this)
    this.onControlClickOK = this.onControlClickOK.bind(this)
    this.onControlClickNOK = this.onControlClickNOK.bind(this)

    this.onClickSave = this.onClickSave.bind(this)
    this.onHandleDateChange = this.onHandleDateChange.bind(this)
  }

  onHandleDateChange(d) {
    this.setState({ ...this.state, startDate: d })
  }

  onControlClick(key, bool) {
    const me = this
    return () => {
      if (me.state[key] != null && me.state[key] === bool) {
        me.setState({ ...me.state, [key]: null })
      } else {
        me.setState({ ...me.state, [key]: bool })
      }
    }
  }

  onControlClickOK(key) {
    return this.onControlClick(key, true)
  }

  onControlClickNOK(key) {
    return this.onControlClick(key, false)
  }

  oneStateIsNotOK() {
    return Object.keys(this.state).filter((k) => k.endsWith('OK') && this.state[k] === false).length > 0
  }

  onClickSave() {
    if (this.oneStateIsNotOK()) {
      // Could extract it, but its only used here and in the method above
      const controls = Object.keys(this.state)
          .filter((k) => k.endsWith('OK') && this.state[k] !== null && typeof this.state[k] !== 'undefined')
          .map((k) => ({
            [k]: this.state[k]
          }));
      // Create a nice representation of the control state
      const controlState = {
        ...flatten(controls),
        doneBy: this.state.user,
        doneDate: this.state.startDate
      };
      // push a new path onto the history, with the provided nice control state
      hashHistory.push({
        pathname: '/observation/control/add',
        state: controlState
      })
    } else {
      this.props.saveControl(this.state)
    }
  }

  getDate() {
    return moment().format('mm/dd/yyyy');
  }

  render() {
    const { translate } = this.props

    const renderReadOnly = (leftValue, rightValue) => {
      const make = (v) => <FormControl style={{ backgroundColor: '#f2f2f2' }} readOnly value={v} />

      if (leftValue && rightValue) {
        return (<div>
          <Col md={5}>
            {make(leftValue)}
          </Col>
          <Col md={4}>
            {make(rightValue)}
          </Col>
        </div>)
      }

      if (leftValue) {
        return <Col md={9}>{make(leftValue)}</Col>
      }

      return (<Col md={9}> --- </Col>)
    }

    const btnTbr = (<SaveCancel
      saveLabel={translate(this.oneStateIsNotOK() ? 'musit.observation.registerObservation' : 'musit.texts.save')}
      translate={translate}
      onClickSave={this.onClickSave}
      onClickCancel={(a) => (a)}
    />)

    const fields = [
      {
        key: 'temperature',
        leftValue: this.state.temperature,
        rightValue: this.state.temperatureTolerance
      },
      {
        key: 'relativeHumidity',
        leftValue: this.state.relativeHumidity,
        rightValue: this.state.relativeHumidityInterval
      },
      {
        key: 'inertAir',
        leftValue: this.state.inertAir,
        rightValue: this.state.inertAirInterval
      },
      {
        key: 'lightConditions',
        leftValue: this.state.light
      },
      {
        key: 'cleaning',
        leftValue: this.state.cleaning
      },
      { key: 'gas' },
      { key: 'alcohol' },
      { key: 'mold' },
      { key: 'pest' }
    ]

    return (
      <div>
        <Grid>
          <Row className="row-centered" style={{ paddingLeft: 40 }}>
            <PageHeader>
              {this.props.translate('musit.newControl.title', false)}
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
                    id={"user"}
                    value={this.state.user}
                    onChange={(v) => { this.setState({ ...this.state, user: v }) }}
                  />
                </Col>
              </Row>
            </Col>
          </Row>

          {fields.map((e, i) => {
            return (
              <Row key={i}>
                <hr />
                <Col md={3}>
                  <PairedToogleButtons
                    label={translate(`musit.newControl.${e.key}`)}
                    value={this.state[`${e.key}OK`]}
                    updatevalueOK={this.onControlClickOK(`${e.key}OK`)}
                    updatevalueNotOK={this.onControlClickNOK(`${e.key}OK`)}
                  />
                </Col>
                <Col md={9}>
                  <Row>
                    <Col md={5}>
                      <label> {translate('musit.newControl.envdata')} </label>
                    </Col>
                  </Row>
                  <Row>
                    {renderReadOnly(e.leftValue, e.rightValue)}
                  </Row>
                </Col>
              </Row>
            )
          })}

          <hr />

          {btnTbr}
        </Grid>
      </div>
    )
  }
}
