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

import React, { Component, PropTypes } from 'react'
import { ObservationDoubleTextAreaComponent } from './index'
import { MusitField, MusitDropDownField } from '../../components/formfields'
import { FormGroup, ControlLabel, Row, Col, Button } from 'react-bootstrap'
import FontAwesome from 'react-fontawesome'

export default class ObservationPest extends Component {
  static propTypes = {
    id: PropTypes.string.isRequired,
    translate: PropTypes.func.isRequired,
    onChangeLifeCycle: PropTypes.func.isRequired,
    lifeCycleItems: PropTypes.array.isRequired,
    onChangeCount: PropTypes.func.isRequired,
    onChangeIdentification: PropTypes.func.isRequired,
    onChangeComments: PropTypes.func.isRequired,
    observations: PropTypes.array.isRequired,
    identificationValue: PropTypes.string.isRequired,
    commentsValue: PropTypes.string.isRequired,
    onAddPest: PropTypes.func.isRequired
  }

  constructor(props) {
    super(props)
    const {
      id,
      lifeCycleItems,
      onChangeIdentification,
      onChangeComments,
      translate
    } = props
    this.fields = {
      lifeCycle: {
        placeHolder: translate('musit.texts.makeChoice'),
        tooltip: translate('musit.observation.pest.lifeCycleTooltip'),
        validate: 'text',
        items: lifeCycleItems,
        translate: translate
      },
      count: {
        placeHolder: translate('musit.observation.pest.countPlaceHolder'),
        tooltip: translate('musit.observation.pest.countTooltip'),
        validate: 'number',
        precision: 3
      },
      comments: {
        id: `${id}_comments`,
        translate: translate,
        leftLabel: translate('musit.observation.pest.identificationLabel'),
        leftTooltip: translate('musit.observation.pest.identificationTooltip'),
        onChangeLeft: onChangeIdentification,
        rightLabel: translate('musit.observation.pest.commentsLabel'),
        rightTooltip: translate('musit.observation.pest.commentsTooltip'),
        onChangeRight: onChangeComments
      }
    }
  }

  render() {
    const { lifeCycle, count, comments } = this.fields
    const {
      id,
      observations,
      identificationValue,
      commentsValue,
      onAddPest,
      translate,
      onChangeLifeCycle,
      onChangeCount
    } = this.props

    const conditionalRender = (shouldRender, body) => {
      let retVal = ''
      if (shouldRender) {
        retVal = body(shouldRender)
      }
      return retVal
    }
    const renderObservation = (row, observation, withAddButton) => {
      return (
        <Row key={row}>
          <Col xs={12} sm={3} md={3}>
            {conditionalRender(observation, (obs) => (
              <span>
                <ControlLabel>{translate('musit.observation.pest.lifeCycleLabel')}</ControlLabel>
                <MusitDropDownField
                  {...lifeCycle}
                  id={`${id}_lifeCycle_${row}`}
                  value={obs.lifeCycle}
                  onChange={(lifeCycleValue) => onChangeLifeCycle(row, lifeCycleValue)}
                />
              </span>
            ))}
          </Col>
          <Col xs={12} sm={3} md={3}>
            {conditionalRender(observation, (obs) => (
              <span>
                <ControlLabel>{translate('musit.observation.pest.countLabel')}</ControlLabel>
                <MusitField
                  {...count}
                  id={`${id}_count_${row}`}
                  value={obs.count}
                  onChange={(countValue) => onChangeCount(row, countValue)}
                />
              </span>
            ))}
          </Col>
          <Col xs={12} sm={6} md={6}>
            {conditionalRender(withAddButton, () => (
              <span>
                <br />
                <Button onClick={() => onAddPest()}>
                  <FontAwesome name="plus-circle" /> {translate('musit.observation.newButtonLabel')}
                </Button>
              </span>
            ))}
          </Col>
        </Row>
      )
    }
    const observationPart = (observations && observations.length > 0)
      ? observations.map((observation, index) => renderObservation(index, observation, index === 0))
      : renderObservation(0, null, true)
    return (
      <div>
        <ObservationDoubleTextAreaComponent {...comments} leftValue={identificationValue} rightValue={commentsValue} />
        {observationPart}
      </div>
    )
  }
}
