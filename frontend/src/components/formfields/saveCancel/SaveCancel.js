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
import { Button, Row, Col } from 'react-bootstrap'

export default class SaveCancel extends Component {
  static propTypes = {
    id: PropTypes.string,
    translate: PropTypes.func.isRequired,
    saveLabel: PropTypes.string,
    saveDisabled: PropTypes.bool,
    onClickSave: PropTypes.func.isRequired,
    cancelLabel: PropTypes.string,
    cancelDisabled: PropTypes.bool,
    onClickCancel: PropTypes.func.isRequired,
  }

  constructor(props) {
    super(props)
    const {
      id,
      saveDisabled,
      onClickSave,
      cancelDisabled,
      onClickCancel
    } = props
    this.fields = {
      save: {
        id: `Save_${id || 1}`,
        onClick: onClickSave,
        disabled: saveDisabled
      },
      cancel: {
        id: `Cancel_${id}`,
        onClick: onClickCancel,
        disabled: cancelDisabled
      }
    }
  }

  render() {
    const { save, cancel } = this.fields
    const { saveLabel, cancelLabel, translate } = this.props
    const showButton = (data) => {
      return (
        <Col xs={6} sm={5} style={{ border: 'none', textAlign: 'center' }}>
          {data}
        </Col>
      )
    }

    return (
      <Row>
        {showButton(<Button {...save}>{saveLabel || translate('musit.texts.save')}</Button>)}
        {showButton(<Button {...cancel}>{cancelLabel || translate('musit.texts.cancel')}</Button>)}
      </Row>
    )
  }
}
