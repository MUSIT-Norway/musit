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

import React, { Component } from 'react';
import TextField from '../../components/musittextfield';
import Options from '../../components/storageunits/EnvironmentOptions';
import { Panel, Form, Grid, Row, PageHeader, Col } from 'react-bootstrap'

export default class EnvironmentOptionsView extends Component {
  static validateString(value, minimumLength = 3, maximumLength = 20) {
    const isSomething = value.length >= minimumLength
    const isValid = isSomething ? 'success' : null
    return value.length > maximumLength ? 'error' : isValid
  }

  static validateNumber(value, minimumLength = 1) {
    const isSomething = value.length >= minimumLength
    const isValid = isSomething ? 'success' : null
    return isSomething && isNaN(value) ? 'error' : isValid
  }

  constructor(props) {
    super(props)

    this.state = {
      name: 'your name',
      description: 'dddd',
      note: '',
      areal: '',
      type: 'Rom',
      skallsikring: false,
      tyverisikring: false,
      brannsikring: true,
      vannskaderisiko: false,
      rutinerBeredskap: false,
      luftfuktighet: false,
      lysforhold: false,
      temperatur: false,
      preventivKonservering: false,
    }
  }

  render() {
    return (
            <div>
                <main>
                    <Panel>
                        <Grid>
                            <Row styleClass="row-centered">
                                <PageHeader>
                                    Welcome to example view.
                                </PageHeader>
                                <Options
                                  unit={this.state}
                                  updateSkallsikring={(enabled) => this.setState({ skallsikring: enabled })}
                                  updateTyverisikring={(enabled) => this.setState({ tyverisikring: enabled })}
                                  updateBrannsikring={(enabled) => this.setState({ brannsikring: enabled })}
                                  updateVannskaderisiko={(enabled) => this.setState({ vannskaderisiko: enabled })}
                                  updateRutinerBeredskap={(enabled) => this.setState({ rutinerBeredskap: enabled })}
                                  updateLuftfuktighet={(enabled) => this.setState({ luftfuktighet: enabled })}
                                  updateLysforhold={(enabled) => this.setState({ lysforhold: enabled })}
                                  updateTemperatur={(enabled) => this.setState({ temperatur: enabled })}
                                  updatePreventivKonservering={(enabled) => this.setState({ preventivKonservering: enabled })}
                                />
                            </Row>
                        </Grid>
                    </Panel>
                </main>
            </div>
        );
  }
}
