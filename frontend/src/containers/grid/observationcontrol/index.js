

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
import { ObservationControlGrid } from '../../../components/grid'
import ObservationControlComponent from '../../../components/leftmenu/observationcontrol'
import Language from '../../../components/language'
import Layout from '../../../layout'
import { connect } from 'react-redux'

const mapStateToProps = (state) => ({
  translate: (key, markdown) => Language.translate(key, markdown),
  observationControlGridData: state.observationControlGrid.data,
  unit: {
    id: 1,
    name: 'Reol 5'
  }
})

@connect(mapStateToProps)
export default class ObservationControlGridShow extends React.Component {
  static propTypes = {
    unit: React.PropTypes.object.isRequired,
    translate: React.PropTypes.func.isRequired,
    observationControlGridData: React.PropTypes.arrayOf(React.PropTypes.object)
  }

  render() {
    const props = this.props;
    return (
      <Layout
        title={`${props.unit.name}${props.translate('musit.grid.observation.header')}`}
        translate={props.translate}
        leftMenu={<ObservationControlComponent
          id={props.unit.id}
          translate={props.translate}
          onClickNewObservation={(key) => key}
          onClickNewControl={(key) => key}
          onClickSelectObservation={(key) => key}
          onClickSelectControl={(key) => key}
        />}
        content={<ObservationControlGrid
          id={props.unit.id}
          translate={props.translate}
          tableData={props.observationControlGridData}
        />}
      />
    )
  }
}
