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

import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';
import { Jumbotron, Panel, Button, Grid, Row, Col, PageHeader } from 'react-bootstrap'
import { Link } from 'react-router';
import { I18n, Translate } from 'react-i18nify'

@connect(null)
export default class Welcome extends Component {

  render() {
        I18n.loadTranslations(require("./language.json"))
        I18n.setLocale("no")
    	  return (
    		<div>
                <main>
                    <Panel>
                        <Grid>
                            <Row styleClass="row-centered">
                                <Col xs={10} md={10}  style={{textAlign: "center"}}>
                                    <PageHeader><Translate value="musit.welcomePage.title" /></PageHeader>
                                    <img src="placeholder-image.png" />
                                    <p><Translate value="musit.welcomePage.body" /></p>
                                </Col>
                            </Row>
                            {/* TODO: redux statecheck for user set and user access */}
                            <Row styleClass="row-centered">
                                <Col  xs={10} md={10} style={{textAlign: "center"}}>
                                    <p><Translate value="musit.welcomePage.noAccess" /></p>
                                </Col>
                            </Row>
                            <Row styleClass="row-centered">
                                <Col xs={10} md={10} style={{textAlign: "center"}}>
                                    <Button bsStyle="default"><img height="11" src="feide-login-icon.png" /> <Translate value="musit.login" /></Button>
                                </Col>
                            </Row>
                        </Grid>
                    </Panel>
                </main>
 		    </div>
    	);
    }
}
