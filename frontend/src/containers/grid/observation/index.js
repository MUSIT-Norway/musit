

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
import { Grid, Row, Col } from 'react-bootstrap'
import { ObservationGrid } from '../../../components/grid'
import Language from '../../../components/language'
import { connect } from 'react-redux'

const mapStateToProps = (state) => ({
  translate: (key, markdown) => Language.translate(key, markdown),
  observationGridData: state.observationGrid.data
})

@connect(mapStateToProps)
export default class ObservationGridShow extends React.Component {
  static propTypes = {
    translate: React.PropTypes.func.isRequired,
    observationGridData: React.PropTypes.arrayOf(React.PropTypes.object)
  }

  render() {
    const { translate } = this.props
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
              <Col sm={8} smOffset={2}>
                <ObservationGrid
                  id="Reol 5"
                  translate={translate}
                  tableData={Object.keys(this.props.observationGridData).map(x => this.props.observationGridData[x])}
                />
              </Col>
            </Row>
          </Grid>
        </main>
      </div>
    )
  }
}
