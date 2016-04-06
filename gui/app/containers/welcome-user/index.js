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
import {  Panel, Grid, Row, Col } from 'react-bootstrap';
import { connect } from 'react-redux';
import { Link } from 'react-router';

@connect()
export default class WelcomeUser extends Component {

  render() {

    	  return (
                <div>
                <Grid>
                    <Row styleClass="row-centered">
                        <Col xs={10} md={10}>
                            <br/>
                            <p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas arcu est, tempus eu tellus et, mollis maximus elit. Morbi est lorem, molestie fermentum interdum a, faucibus ultricies lectus. Ut eu lorem mi. In dictum neque vitae mollis posuere. Sed maximus, est eget rhoncus sagittis, dolor enim varius felis, at hendrerit urna urna a justo. Proin nunc ex, tristique sed bibendum vel, porta et elit. Vestibulum consectetur lacus nec elementum ornare. Vestibulum venenatis condimentum tortor sit amet pharetra. Maecenas tempus lectus vel vestibulum pharetra. Suspendisse euismod condimentum massa.</p>

                            <p>In at scelerisque erat, non venenatis nisl. Mauris sagittis odio id lectus faucibus, sit amet faucibus mauris gravida. Interdum et malesuada fames ac ante ipsum primis in faucibus. Etiam iaculis cursus ante ut dignissim. Sed finibus suscipit ultrices. Integer sit amet malesuada odio. Phasellus vel commodo purus. Cras ultrices tincidunt nisi, at vehicula justo luctus at. Mauris pulvinar fringilla condimentum. Proin sed lacus nec urna lacinia porttitor in ac augue.</p>

                            <p>Vestibulum sed viverra augue. Nam iaculis mattis dui a maximus. Curabitur eget magna id velit pulvinar gravida. In rutrum urna ac semper auctor. Integer vel viverra metus. Sed dignissim dictum sapien non volutpat. Nunc nec auctor nunc, sit amet sodales lectus. Morbi cursus ipsum at odio fringilla, dictum scelerisque ligula lobortis. Vestibulum congue leo non est ultrices feugiat. Vestibulum venenatis nisi nisi.</p>

                            <p>Mauris scelerisque nulla et tempus laoreet. Nullam pharetra ultricies ipsum, a feugiat tellus fermentum at. Donec quis ipsum at eros viverra aliquam. Nulla facilisi. Aliquam erat volutpat. Nunc vel ante quis neque accumsan gravida. Vestibulum vitae urna vitae turpis tristique posuere et a lacus. Nulla viverra aliquam viverra. Nam eu diam tellus. Donec porta tellus odio, sed ultricies leo vestibulum sit amet. Proin et mauris condimentum enim cursus ornare. Pellentesque posuere eu nulla non posuere. Morbi id lobortis est. Morbi elementum pellentesque metus, eget fermentum purus hendrerit at. Nam ullamcorper est odio, vitae sodales elit porttitor vel.</p>

                            <p>Cras dapibus purus lorem, sit amet consectetur orci ornare ut. Integer vitae nulla orci. Pellentesque vestibulum sed leo sed posuere. Nulla blandit leo velit, vitae elementum neque porttitor id. Nulla ex diam, aliquet non maximus eget, sodales et purus. Fusce eu neque quis erat viverra facilisis vitae at sem. Morbi egestas, velit eget volutpat convallis, elit nunc ultricies dolor, eget finibus diam arcu a lacus. Phasellus tristique magna mattis elementum sodales. Sed luctus lorem non libero porttitor mollis. Cras at velit rhoncus, porttitor lectus eu, mattis est. Morbi vel tempor felis. Cras sodales eros nec lacus bibendum tristique. Nulla efficitur, erat faucibus viverra sagittis, erat orci porta lorem, posuere auctor nibh sapien ullamcorper sapien. Vivamus ornare quam vel ipsum feugiat, lacinia mollis mi suscipit.</p>
                        </Col>
                    </Row>
                </Grid>
</div>
    	);
    }
}
