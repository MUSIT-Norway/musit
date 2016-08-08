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

import React, { Component } from 'react'
import { Panel, Grid, Row, Col } from 'react-bootstrap'
import { connect } from 'react-redux'
import Language from '../../components/language'

const mapStateToProps = () => ({
  translate: (key, markdown) => Language.translate(key, markdown)
})

@connect(mapStateToProps)
export default class WelcomeUser extends Component {
  static propTypes = {
    translate: React.PropTypes.func.isRequired,
  }
  render() {
    return (
      <div>
        <Grid>
          <Row className="row-centered">
            <Col xs={10} md={10}>
              <br />
              <Panel>
                {this.props.translate('musit.welcomeUserPage.body', true)}
              </Panel>
            </Col>
          </Row>
        </Grid>
      </div>
    )
  }
}
