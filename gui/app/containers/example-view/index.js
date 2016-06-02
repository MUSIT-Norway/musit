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
import Autosuggest from 'react-autosuggest'
import AutosuggestHighlight from 'autosuggest-highlight'

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
        areal: '',
        type: 'Rom'
      },
      suggestions: [],
      value: ''
    }

    const clazz = this

    this.areal = {
      controlId: 'areal',
      labelText: 'Areal',
      tooltip: 'Areal',
      valueType: 'text',
      placeHolderText: 'enter areal here',
      valueText: () => clazz.state.unit.areal,
      validationState: () => ExampleView.validateNumber(clazz.state.unit.areal),
      onChange: (areal) => clazz.setState({ unit: { ...this.state.unit, areal } })
    }

    this.fields = [
      {
        controlId: 'name',
        labelText: 'Name',
        tooltip: 'Name',
        valueType: 'text',
        placeHolderText: 'enter name here',
        valueText: () => clazz.state.unit.name,
        validationState: () => ExampleView.validateString(clazz.state.unit.name),
        onChange: (name) => clazz.setState({ unit: { ...clazz.state.unit, name } })
      },
      {
        controlId: 'description',
        labelText: 'Description',
        tooltip: 'Description',
        placeHolderText: 'enter description here',
        valueType: 'text',
        valueText: () => clazz.state.unit.description,
        validationState: () => ExampleView.validateString(clazz.state.unit.description),
        onChange: (description) => clazz.setState({ unit: { ...clazz.state.unit, description } })
      },
      {
        controlId: 'note',
        labelText: 'Note',
        tooltip: 'Note',
        valueType: 'text',
        placeHolderText: 'enter note here',
        valueText: () => clazz.state.unit.note,
        validationState: () => ExampleView.validateString(clazz.state.unit.note),
        onChange: (note) => clazz.setState({ unit: { ...clazz.state.unit, note } })
      }
    ]
    this.onSuggestionsUpdateRequested = this.onSuggestionsUpdateRequested.bind(this)
    this.onChange = this.onChange.bind(this)
  }

  onSuggestionsUpdateRequested({ value }) {
    if (value && value.length >= 3) {
      this.setState({
        suggestions: this.getSuggestions()
      })
    } else {
      this.setState({
        suggestions: []
      })
    }
  }

  getSuggestions() {
    return ['aaaa', 'bbbb', 'cccc']
  }

  getSuggestionValue(suggestion) {
    return suggestion
  }

  renderSuggestion(suggestion, { value, valueBeforeUpDown }) {
    console.log(suggestion)
    console.log(value)
    console.log(valueBeforeUpDown)
    const suggestionText = `${suggestion}`
    const query = (valueBeforeUpDown || value).trim()
    const matches = AutosuggestHighlight.match(suggestionText, query)
    const parts = AutosuggestHighlight.parse(suggestionText, matches)

    return (
      <span>
        <span>
          {
            parts.map((part, index) => {
              const className = part.highlight ? 'highlight' : null;

              return (
                <span className={className} key={index}>{part.text}</span>
              )
            })
          }
        </span>
      </span>
    )
  }

  onChange(event, { newValue, method }) {
    this.setState({
      value: newValue
    })
  }

  render() {
    const { suggestions, value } = this.state
    const inputProps = {
      placeholder: 'Type anything more then 3 letters',
      value,
      onChange: this.onChange
    }

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
                    {this.fields.map(field => <TextField {...field} />)}
                  </Form>
                </Col>
                <Col md={6}>
                  <Form horizontal>
                    <TextField {...this.areal} />
                    <TextField {...this.areal} />
                    <TextField {...this.areal} />
                  </Form>
                </Col>
              </Row>
              <Row>
                <Col md={12}>
                  <Autosuggest suggestions={suggestions}
                    onSuggestionsUpdateRequested={this.onSuggestionsUpdateRequested}
                    getSuggestionValue={this.getSuggestionValue}
                    renderSuggestion={this.renderSuggestion}
                    inputProps={inputProps}
                  />
                </Col>
              </Row>
            </Grid>
          </Panel>
        </main>
      </div>
    );
  }
}
