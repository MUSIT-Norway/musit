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
import { Panel, Form, Grid, Row, PageHeader, Col } from 'react-bootstrap'

export default class ExampleView extends Component {
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
      unit: {
        name: 'your name',
        description: 'dddd',
        note: '',
        areal: ''
      }
    }

    const clazz = this

    this.areal = {
      controlId: 'areal',
      labelText: 'Areal',
      tooltip: 'Areal',
      valueText: clazz.state.unit.areal,
      valueType: 'text',
      validationState: () => ExampleView.validateNumber(clazz.state.unit.areal),
      placeHolderText: 'enter areal here',
      onChange: (areal) => clazz.setState({ unit: { ...this.state.unit, areal } })
    }

    this.fields = [
      {
        controlId: 'name',
        labelText: 'Name',
        tooltip: 'Name',
        valueText: clazz.state.unit.name,
        valueType: 'text',
        validationState: () => ExampleView.validateString(clazz.state.unit.name),
        placeHolderText: 'enter name here',
        onChange: (name) => clazz.setState({ unit: { ...clazz.state.unit, name } })
      },
      {
        controlId: 'description',
        labelText: 'Description',
        tooltip: 'Description',
        valueText: clazz.state.unit.description,
        valueType: 'text',
        validationState: () => ExampleView.validateString(clazz.state.unit.description),
        placeHolderText: 'enter description here',
        onChange: (description) => clazz.setState({ unit: { ...clazz.state.unit, description } })
      },
      {
        controlId: 'note',
        labelText: 'Note',
        tooltip: 'Note',
        valueText: clazz.state.unit.note,
        valueType: 'text',
        validationState: () => ExampleView.validateString(clazz.state.unit.note),
        placeHolderText: 'enter note here',
        onChange: (note) => clazz.setState({ unit: { ...clazz.state.unit, note } })
      }
    ]
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
                <Col md={6}>
                  <Form horizontal>
                    {this.fields.map(field =>
                      <TextField
                        controlId={field.controlId}
                        labelText={field.labelText}
                        tooltip={field.tooltip}
                        valueText={this.state.unit[field.controlId]}
                        valueType={field.valueType}
                        validationState={field.validationState}
                        placeHolderText={field.placeHolderText}
                        onChange={field.onChange}
                      />
                    )}
                  </Form>
                </Col>
                <Col md={6}>
                  <Form horizontal>
                    <TextField
                      controlId={this.areal.controlId}
                      labelText={this.areal.labelText}
                      tooltip={this.areal.tooltip}
                      valueText={this.state.unit[this.areal.controlId]}
                      valueType={this.areal.valueType}
                      validationState={this.areal.validationState}
                      placeHolderText={this.areal.placeHolderText}
                      onChange={this.areal.onChange}
                    />
                    <TextField
                      controlId={this.areal.controlId}
                      labelText={this.areal.labelText}
                      tooltip={this.areal.tooltip}
                      valueText={this.state.unit[this.areal.controlId]}
                      valueType={this.areal.valueType}
                      validationState={this.areal.validationState}
                      placeHolderText={this.areal.placeHolderText}
                      onChange={this.areal.onChange}
                    />
                    <TextField
                      controlId={this.areal.controlId}
                      labelText={this.areal.labelText}
                      tooltip={this.areal.tooltip}
                      valueText={this.state.unit[this.areal.controlId]}
                      valueType={this.areal.valueType}
                      validationState={this.areal.validationState}
                      placeHolderText={this.areal.placeHolderText}
                      onChange={this.areal.onChange}
                    />
                  </Form>
                </Col>
              </Row>
            </Grid>
          </Panel>
        </main>
      </div>
    );
  }
}
