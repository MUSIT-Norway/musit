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
import {
  ObservationDoubleTextAreaComponent,
  ObservationFromToNumberCommentComponent,
  ObservationStatusPercentageComment,
  ObservationPest
}
  from '../../../components/observation'
import { Panel, Grid, Row, Col } from 'react-bootstrap'
import { connect } from 'react-redux'
import Language from '../../../components/language'

const mapStateToProps = () => ({
  translate: (key, markdown) => Language.translate(key, markdown)
})

@connect(mapStateToProps)
export default class ObservationView extends React.Component {
  static propTypes = {
    translate: React.PropTypes.func.isRequired
  }

  constructor(props) {
    super(props)
    this.state = {
      pestObservations: []
    }

    this.addPest = this.addPest.bind(this)
  }

  addPest() {
    this.setState({ ...this.state, pestObservations: [...this.state.pestObservations, { lifeCycle: '', count: 0 }] })
  }

  render() {
    const { translate } = this.props
    const { pestObservations } = this.state

    const statusOptions = ['Uttørket', 'Nesten uttørket', 'Noe uttørket', 'Litt uttørket', 'Tilfredstillende']
    return (
      <div>
        <main>
          <Panel>
            <Grid>
              <Row>
                <Col sm={10} smOffset={1}>
                  <ObservationPest
                    id="test6"
                    translate={translate}
                    onChangeLifeCycle={(index, value) => (`${index}:${value}`)}
                    lifeCycleItems={['Adult', 'Puppe', 'Puppeskinn', 'Larva', 'Egg']}
                    onChangeCount={(index, value) => (`${index}:${value}`)}
                    onChangeIdentification={() => ('Comment changed')}
                    onChangeComments={() => ('Comment changed')}
                    observations={pestObservations}
                    identificationValue="ID"
                    commentsValue=""
                    onAddPest={() => this.addPest()}
                  />
                </Col>
              </Row>
              <Row>
                <Col sm={10} smOffset={1}>
                  <ObservationStatusPercentageComment
                    id="test1"
                    translate={translate}
                    statusLabel="Tilstand"
                    statusValue="122,123"
                    statusTooltip="From tooltip"
                    statusOptionValues={statusOptions}
                    onChangeStatus={() => ('From changed')}
                    volumeLabel="Volum %"
                    volumeValue="123"
                    volumeTooltip="To tooltip"
                    onChangeVolume={() => ('TO changed')}
                    commentLabel="Tiltak/Kommerntar"
                    commentValue=""
                    commentTooltip="Comment tooltip"
                    onChangeComment={() => ('Comment changed')}
                  />
                </Col>
              </Row>
              <Row>
                <Col sm={10} smOffset={1}>
                  <ObservationFromToNumberCommentComponent
                    id="test2"
                    translate={translate}
                    fromLabel="from label"
                    fromValue="122,123"
                    fromTooltip="From tooltip"
                    onChangeFrom={() => ('From changed')}
                    toLabel="To labela"
                    toValue="1234"
                    toTooltip="To tooltip"
                    onChangeTo={() => ('TO changed')}
                    commentLabel="Comment label"
                    commentValue="1234"
                    commentTooltip="Comment tooltip"
                    onChangeComment={() => ('Comment changed')}
                  />
                </Col>
              </Row>
              <Row>
                <Col sm={10} smOffset={1}>
                  <ObservationDoubleTextAreaComponent
                    id="test3"
                    translate={translate}
                    leftLabel="Left label"
                    leftValue="Left test value"
                    leftTooltip="Left tooltip"
                    onChangeLeft={() => ('left changed')}
                    rightLabel="Right label"
                    rightValue="right"
                    rightTooltip="Right tooltip"
                    onChangeRight={() => ('Right changed')}
                  />
                </Col>
              </Row>
            </Grid>
          </Panel>
        </main>
      </div>
    )
  }
}
