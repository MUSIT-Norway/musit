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
import { Panel, Form, Grid, Row, PageHeader } from 'react-bootstrap'

export default class ExampleView extends Component {
  constructor(props) {
    super(props)

    this.updateName = this.updateName.bind(this)
    this.validateField = this.validateField.bind(this)
    this.updateDescription = this.updateDescription.bind(this)
    this.updateNote = this.updateNote.bind(this)
    this.updateAreal = this.updateAreal.bind(this)

    const clazz = this

    this.state = {
      unit: {
        name: 'your name',
        description: 'dddd',
        note: '',
        areal: ''
      }
    }

    this.areal = {
      controlId: 'areal',
      labelText: 'Areal',
      tooltip: 'Areal',
      valueText: clazz.state.unit.name,
      valueType: 'text',
      validationState: clazz.validateField,
      placeHolderText: 'enter areal here',
      onChange: clazz.updateAreal
    }

    this.fields = [
      {
        controlId: 'name',
        labelText: 'Name',
        tooltip: 'Name',
        valueText: clazz.state.unit.name,
        valueType: 'text',
        validationState: clazz.validateField,
        placeHolderText: 'enter name here',
        onChange: clazz.updateName
      },
      {
        controlId: 'description',
        labelText: 'Description',
        tooltip: 'Description',
        valueText: clazz.state.unit.description,
        valueType: 'text',
        validationState: clazz.validateField,
        placeHolderText: 'enter description here',
        onChange: clazz.updateDescription
      },
      {
        controlId: 'note',
        labelText: 'Note',
        tooltip: 'Note',
        valueText: clazz.state.unit.note,
        valueType: 'text',
        validationState: clazz.validateField,
        placeHolderText: 'enter note here',
        onChange: clazz.updateNote
      }
    ]
  }

  validateField(value) {
    const notNull = value.length ? 'success' : null
    return value.length > 20 ? 'error' : notNull
  }

  updateName(name) {
    this.setState({ unit: { ...this.state.unit, name } })
  }

  updateDescription(description) {
    this.setState({ unit: { ...this.state.unit, description } })
  }

  updateNote(note) {
    this.setState({ unit: { ...this.state.unit, note } })
  }

  updateAreal(areal) {
    this.setState({ unit: { ...this.state.unit, areal } })
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
                <Form horizontal>
                  <TextField
                    controlId={this.areal.controlId}
                    labelText={this.areal.labelText}
                    tooltip={this.areal.tooltip}
                    valueText={this.state.unit[this.areal.controlId]}
                    valueType={this.areal.valueType}
                    validationState={this.areal.validationState(this.state.unit[this.areal.controlId])}
                    placeHolderText={this.areal.placeHolderText}
                    onChange={this.areal.onChange}
                  />
                  {this.fields.map(field =>
                    <TextField
                      controlId={field.controlId}
                      labelText={field.labelText}
                      tooltip={field.tooltip}
                      valueText={this.state.unit[field.controlId]}
                      valueType={field.valueType}
                      validationState={field.validationState(this.state.unit[field.controlId])}
                      placeHolderText={field.placeHolderText}
                      onChange={field.onChange}
                    />
                  )}
                </Form>
              </Row>
            </Grid>
          </Panel>
        </main>
      </div>
    );
  }
}
