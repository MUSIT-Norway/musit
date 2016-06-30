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
import { MusitField, MusitTextArea, MusitDropDownField } from '../../components/formfields'
import { FormGroup, ControlLabel, Col } from 'react-bootstrap'

export default class ObervationStatusPercentageComment extends Component {
  static propTypes = {
    id: PropTypes.string.isRequired,
    translate: PropTypes.func,
    statusLabel: PropTypes.string,
    statusValue: PropTypes.string.isRequired,
    statusOptionValues: PropTypes.array.isRequired,
    statusTooltip: PropTypes.string,
    onChangeStatus: PropTypes.func.isRequired,
    volumeLabel: PropTypes.string,
    volumeValue: PropTypes.string.isRequired,
    volumeTooltip: PropTypes.string,
    onChangeVolume: PropTypes.func.isRequired,
    commentLabel: PropTypes.string,
    commentValue: PropTypes.string.isRequired,
    commentTooltip: PropTypes.string,
    onChangeComment: PropTypes.func.isRequired
  }

  constructor(props) {
    super(props)
    const {
      id,
      statusTooltip,
      onChangeStatus,
      volumeTooltip,
      statusOptionValues,
      onChangeVolume,
      commentTooltip,
      onChangeComment,
      translate
    } = props
    this.fields = {
      status: {
        id: `${id}_status`,
        placeHolder: translate('musit.texts.makeChoice'),
        tooltip: statusTooltip,
        onChange: onChangeStatus,
        validate: 'text',
        precision: 3,
        items: statusOptionValues,
        translate: this.props.translate
      },
      volume: {
        id: `${id}_percentage`,
        placeHolder: '%',
        tooltip: volumeTooltip,
        onChange: onChangeVolume,
        validate: 'number',
        precision: 3
      },
      comment: {
        id: `${id}_comment`,
        placeHolder: translate('musit.texts.freetext'),
        tooltip: commentTooltip,
        onChange: onChangeComment,
        validate: 'text',
        maximumLength: 250,
        numberOfRows: 5
      }
    }
  }

  render() {
    const { status, volume, comment } = this.fields
    const { statusValue, volumeValue, statusLabel, volumeLabel, commentValue, commentLabel } = this.props

    return (
      <FormGroup>
        <Col xs={12} sm={3}>
          <ControlLabel>{statusLabel}</ControlLabel>
          <MusitDropDownField {...status} value={statusValue} />
        </Col>
        <Col xs={12} sm={3}>
          <ControlLabel>{volumeLabel}</ControlLabel>
          <MusitField {...volume} value={volumeValue} />
        </Col>
        <Col xs={12} sm={6}>
          <ControlLabel>{commentLabel}</ControlLabel>
          <MusitTextArea {...comment} value={commentValue} />
        </Col>
      </FormGroup>
    )
  }
}
