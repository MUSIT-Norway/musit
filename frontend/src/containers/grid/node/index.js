

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
import { NodeGrid } from '../../../components/grid'
import Language from '../../../components/language'
import { connect } from 'react-redux'
import { loadNode } from '../../../reducers/grid/node'

const mapStateToProps = (state) => ({
  translate: (key, markdown) => Language.translate(key, markdown),
  nodeGridData: state.nodeGrid.data
})

const mapDispatchToProps = (dispatch) => ({
  loadStorageNode: (id) => {
    dispatch(loadNode(id))
  }
})

@connect(mapStateToProps, mapDispatchToProps)
export default class NodeGridShow extends React.Component {
  static propTypes = {
    params: React.PropTypes.object,
    translate: React.PropTypes.func.isRequired,
    nodeGridData: React.PropTypes.arrayOf(React.PropTypes.object),
    loadStorageNode: React.PropTypes.func.isRequired
  }

  componentWillMount() {
    if (this.props.params.id) {
      this.props.loadStorageNode(this.props.params.id)
    }
  }

  render() {
    const { translate, nodeGridData, loadStorageNode } = this.props
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
                <NodeGrid
                  id="1"
                  translate={translate}
                  tableData={Object.keys(nodeGridData).map(x => nodeGridData[x])}
                  loadNode={loadStorageNode}
                />
              </Col>
            </Row>
          </Grid>
        </main>
      </div>
    )
  }
}
