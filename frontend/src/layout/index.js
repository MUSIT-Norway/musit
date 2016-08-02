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
import { Grid, Row, Col, PageHeader } from 'react-bootstrap'

export default class Layout extends React.Component {
  static propTypes = {
    title: React.PropTypes.string.isRequired,
    translate: React.PropTypes.func.isRequired,
    leftMenu: React.PropTypes.object.isRequired,
    content: React.PropTypes.object.isRequired
  }

  render() {
    return (
      <div>
        <main>
          <Grid>
            <Row>
              <Row>
                <Col style={{ textAlign: 'left', paddingLeft: 40 }}>
                  <PageHeader>
                    {this.props.title}
                  </PageHeader>
                </Col>
              </Row>
            </Row>
            <Row>
              <Col md={2} style={{ paddingTop: 30 }}>
                {this.props.leftMenu}
              </Col>
              <Col md={8}>
                {this.props.content}
              </Col>
            </Row>
          </Grid>
        </main>
      </div>
    )
  }
}
