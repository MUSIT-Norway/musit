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
import { MusitField, MusitTextArea } from '../../components/formfields'
import { Row, ControlLabel, Col } from 'react-bootstrap'

export default class ObservationFromToNumberCommentComponent extends Component {
  static propTypes = {
    id: PropTypes.string.isRequired,
    translate: PropTypes.func.isRequired,
    fromLabel: PropTypes.string.isRequired,
    fromValue: PropTypes.string.isRequired,
    fromTooltip: PropTypes.string.isRequired,
    onChangeFrom: PropTypes.func.isRequired,
    toLabel: PropTypes.string.isRequired,
    toValue: PropTypes.string.isRequired,
    toTooltip: PropTypes.string.isRequired,
    onChangeTo: PropTypes.func.isRequired,
    commentLabel: PropTypes.string.isRequired,
    commentValue: PropTypes.string.isRequired,
    commentTooltip: PropTypes.string.isRequired,
    onChangeComment: PropTypes.func.isRequired,
    fromPlaceHolder: PropTypes.string,
    toPlaceHolder: PropTypes.string,
    disabled: PropTypes.bool
  }

  constructor(props) {
    super(props)
    const { id, fromTooltip, onChangeFrom, toTooltip, onChangeTo, commentTooltip, onChangeComment, translate,
      fromPlaceHolder, toPlaceHolder, disabled } = props
    this.fields = {
      from: {
        id: `${id}_from`,
        placeHolder: fromPlaceHolder,
        tooltip: fromTooltip,
        onChange: onChangeFrom,
        validate: 'number',
        precision: 3,
        disabled: disabled
      },
      to: {
        id: `${id}_to`,
        placeHolder: toPlaceHolder,
        tooltip: toTooltip,
        onChange: onChangeTo,
        validate: 'number',
        precision: 3,
        disabled: disabled
      },
      comment: {
        id: `${id}_comment`,
        placeHolder: translate('musit.texts.freetext'),
        tooltip: commentTooltip,
        onChange: onChangeComment,
        validate: 'text',
        maximumLength: 250,
        numberOfRows: 5,
        disabled: disabled
      }
    }
  }

  render() {
    const { from, to, comment } = this.fields
    const { fromValue, toValue, fromLabel, toLabel, commentValue, commentLabel } = this.props

    return (
      <Row>
        <Col xs={12} sm={3}>
          <ControlLabel>{fromLabel}</ControlLabel>
          <MusitField {...from} value={fromValue} />
        </Col>
        <Col xs={12} sm={3}>
          <ControlLabel>{toLabel}</ControlLabel>
          <MusitField {...to} value={toValue} />
        </Col>
        <Col xs={12} sm={6}>
          <ControlLabel>{commentLabel}</ControlLabel>
          <MusitTextArea {...comment} value={commentValue} />
        </Col>
      </Row>
    )
  }
}
