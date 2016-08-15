

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
import Toolbar from '../../../layout/Toolbar'
import { hashHistory } from 'react-router'

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
    observationControlGridData: React.PropTypes.arrayOf(React.PropTypes.object),
    params: React.PropTypes.object,
  }

  constructor(props) {
    super(props)
    this.state = {
      showControls: true,
      showObservations: true
    }
  }

  makeToolbar() {
    return (<Toolbar
      showRight={this.state.showControls}
      showLeft={this.state.showObservations}
      labelRight="Kontroller"
      labelLeft="Observasjoner"
      placeHolderSearch="Filtrer i liste"
      clickShowRight={() => this.setState({ ...this.state, showControls: !this.state.showControls })}
      clickShowLeft={() => this.setState({ ...this.state, showObservations: !this.state.showObservations })}
    />)
  }

  makeLeftMenu() {
    return (<div style={{ paddingTop: 10 }}>
      <ObservationControlComponent
        id={this.props.unit.id}
        translate={this.props.translate}
        selectObservation
        selectControl
        onClickNewObservation={() => hashHistory.push(`${this.props.params.id}/observation/add`)}
        onClickNewControl={() => hashHistory.push(`${this.props.params.id}/control/add`)}
      />
    </div>)
  }

  makeContent() {
    return (<ObservationControlGrid
      id={this.props.unit.id}
      translate={this.props.translate}
      tableData={this.props.observationControlGridData}
    />)
  }

  render() {
    return (
      <Layout
        title={`${this.props.translate('musit.grid.observation.header')}`}
        translate={this.props.translate}
        breadcrumb={"Museum / Papirdunken / Esken inni der"}
        toolbar={this.makeToolbar()}
        leftMenu={this.makeLeftMenu()}
        content={this.makeContent()}
      />
    )
  }
}
