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
    const { translate } = this.props
    const temperature = {
      type: 'temperature',
      ok: this.props.control.control.temperatureControl.ok
    }
    const relativeHumidity = {
      type: 'relativeHumidity',
      ok: this.props.control.control.relativeHumidity.ok
    }
    const lightCondition = {
      type: 'lightCondition',
      ok: this.props.control.control.lightConditionControl.ok
    }
    const pest = {
      type: 'pest',
      ok: this.props.control.control.pestControl.ok
    }
    const alcohol = {
      type: 'alcohol',
      ok: this.props.control.control.alcoholControl.ok
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
                <ControlLabel>{translate('musit.texts.datePerformed')}</ControlLabel>
                <br />
                <DatePicker dateFormat="DD.MM.YYYY" value={this.props.control.datePerformed} />
              </Col>
              <Col sm={4} >
                <ControlLabel>{translate('musit.texts.performedBy')}</ControlLabel>
                <br />
                <MusitField id="performedBy" value={this.props.control.performedBy} validate="text" disabled={Boolean(true)} />
              </Col>
            </Row>
            <Row>
              <Col sm={4} smOffset={2}>
                <ControlLabel>{translate('musit.texts.dateRegistered')}</ControlLabel>
                <br />
                <DatePicker dateFormat="DD.MM.YYYY" value={this.props.control.dateRegistered} />
              </Col>
              <Col sm={4} >
                <ControlLabel>{translate('musit.texts.registeredBy')}</ControlLabel>
                <br />
                <MusitField id="registeredBy" value={this.props.control.registeredBy} validate="text" disabled={Boolean(true)} />
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
