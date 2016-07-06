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
import { Grid, Row, Col, ControlLabel } from 'react-bootstrap'
import { ControlView } from '../../components/control'
import DatePicker from 'react-bootstrap-date-picker'
import { MusitField } from '../../components/formfields'
import Language from '../../components/language'
import { connect } from 'react-redux'

const mapStateToProps = (state) => ({
  translate: (key, markdown) => Language.translate(key, markdown),
  control: state.control.data
})

@connect(mapStateToProps)
export default class ControlViewShow extends React.Component {
  static propTypes = {
    translate: React.PropTypes.func.isRequired,
    control: React.PropTypes.arrayOf(React.PropTypes.object)
  }

  render() {
    require('react-datepicker/dist/react-datepicker.css');
    const { translate } = this.props
    const temperature = {
      type: 'temperature',
      ok: this.props.control.temperatureControl.ok
    }
    const relativeHumidity = {
      type: 'relativeHumidity',
      ok: this.props.control.relativeHumidity.ok
    }
    const lightCondition = {
      type: 'lightCondition',
      ok: this.props.control.lightConditionControl.ok
    }
    const pest = {
      type: 'pest',
      ok: this.props.control.pestControl.ok
    }
    const alcohol = {
      type: 'alcohol',
      ok: this.props.control.alcoholControl.ok
    }
    return (
      <div>
        <main>
          <Grid>
            <Row>
              <br />
              <br />
              <br />
            </Row>
            <Row>
              <Col sm={4} smOffset={2}>
                <ControlLabel>{translate('musit.texts.dateCompleted')}</ControlLabel>
                <br />
                <DatePicker />
              </Col>
              <Col sm={4} >
                <ControlLabel>{translate('musit.texts.performedBy')}</ControlLabel>
                <br />
                <MusitField id="1" value="test user" validate="text" />
              </Col>
            </Row>
            <Row>
              <Col sm={4} smOffset={2}>
                <ControlLabel>{translate('musit.texts.dateRegistered')}</ControlLabel>
                <br />
                <DatePicker />
              </Col>
              <Col sm={4} >
                <ControlLabel>{translate('musit.texts.registeredBy')}</ControlLabel>
                <br />
                <MusitField id="2" value="test user" validate="text" />
              </Col>
            </Row>
            <Row>
              <br />
            </Row>
            <Row>
              <Col sm={8} smOffset={2}>
                <ControlView
                  id="1"
                  translate={translate}
                  controls={[
                    temperature,
                    relativeHumidity,
                    lightCondition,
                    pest,
                    alcohol
                  ]}
                />
              </Col>
            </Row>
          </Grid>
        </main>
      </div>
    )
  }
}