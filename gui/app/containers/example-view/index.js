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
import MusitTextField from '../../components/musittextfield';
import { Panel, Button, Grid, Row, Col, PageHeader } from 'react-bootstrap'

export default class ExampleView extends Component {
  constructor(props) {
      super(props)
       this.state = {
           name: 'your name',
           desc: 'dddd',
           note: 'note'
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
                            <MusitTextField
                                controlId="name"
                                labelText="Name"
                                valueText={this.state.name}
                                placeHolderText="Bjarne"
                                onChange={(value) => this.setState({name: value})}
                            />
                            <MusitTextField
                                controlId="2"
                                labelText="heihei2"
                                placeHolderText="Bjarne"
                                onChange={(value) => this.setState({name: value})}
                            />
                            <MusitTextField
                                controlId="3"
                                labelText="heihei3"
                                valueText= "other string"
                                placeHolderText="klaus"
                                onChange={(value) => this.setState({name: value})}
                            />
                        </Row>
                    </Grid>
                </Panel>
            </main>
        </div>
      );
  }
}
