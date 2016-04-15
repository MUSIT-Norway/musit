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
import { Jumbotron, Panel, Button, Grid, Row, Col, PageHeader } from 'react-bootstrap';
import { Link } from 'react-router';
import Language from '../../components/language'
import { login } from '../../reducers/auth'

const mapStateToProps = (state) => {
  return {
    user: state.auth.user,
    login: login
  }
}

@connect(mapStateToProps)
export default class Welcome extends Component {
  handleFakeLogin = (event) => {
    this.props.dispatch(this.props.login('fake'))
  }

  render() {
          const styles = require('./index.scss');
          console.log(styles)
    	  return (
    		<div>
                <main>
                    <Panel>
                        <Grid>
                            <Row styleClass="row-centered">
                                <Col xs={10} md={10}  style={{textAlign: "center"}}>
                                    <PageHeader><Language value="musit.welcomePage.title" markdown={true}/></PageHeader>
                                    <div className={styles.panelWithBackground}>
                                        <span style={{fontSize: "larger", margin: "1em"}}>
                                            <Language value="musit.welcomePage.body" markdown={true} />
                                        </span>
                                    </div>
                                </Col>
                            </Row>
                            {/* TODO: redux statecheck for user set and user access */}
                            <Row styleClass="row-centered">
                               {/*} <Col  xs={10} md={10} style={{textAlign: "center"}}>
                                    <Language value="musit.welcomePage.noAccess" />
                                </Col>*/}
                            </Row>
                            {!this.props.user &&
                            <Row styleClass="row-centered">
                                <Col xs={10} md={10} style={{textAlign: "center"}}>
                                    <Button bsStyle="default" style={{marginTop: "1em"}}
                                    onClick={this.handleFakeLogin}><img height="11" src="feide-login-icon.png" /> <Language value="musit.login" /></Button>
                                </Col>
                            </Row>
                            }
                        </Grid>
                    </Panel>
                </main>
 		    </div>
    	);
    }
}

