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
import { connect } from 'react-redux';
import TextField from '../../components/musittextfield'
import Options from '../../components/storageunits/EnvironmentOptions'
import StorageUnitComponents from '../../components/storageunits/StorageUnitComponent'
import EnvironmentRequirementComponent from '../../components/storageunits/EnvironmentRequirementComponent'
import { Panel, Form, Grid, Row, PageHeader, Col } from 'react-bootstrap'
import Language from '../../components/language'
import Autosuggest from 'react-autosuggest'
import { suggestAddress, clearSuggest } from '../../reducers/suggest'

const mapStateToProps = (state) => ({
  suggest: state.suggest,
  translate: (key, markdown) => Language.translate(key, markdown)
})

const mapDispatchToProps = (dispatch) => {
  return {
    onSuggestionsUpdateRequested({ value, reason }) {
      // Should only autosuggest on typing if you have more then 3 characters
      if (reason && (reason === 'type') && value && (value.length >= 3)) {
        dispatch(suggestAddress('addressField', value))
      } else {
        dispatch(clearSuggest('addressField'))
      }
    }
  }
}

@connect(mapStateToProps, mapDispatchToProps)
export default class ExampleView extends Component {
  static propTypes = {
    translate: React.PropTypes.func.isRequired,
    onSuggestionsUpdateRequested: React.PropTypes.func.isRequired,
    suggest: React.PropTypes.array.isRequired
  }

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
        type: 'Lagringsenhet'
      },
      sikringBevaring: {
        skallsikring: false,
        tyverisikring: true,
        brannsikring: true,
        vannskaderisiko: false,
        rutinerBeredskap: false,
        luftfuktighet: false,
        lysforhold: false,
        temperatur: false,
        preventivKonservering: false
      },
      address: ''
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
    this.onChange = this.onChange.bind(this)
  }

  onChange(event, { newValue }) {
    this.setState({
      address: newValue
    })
  }

  getSuggestionValue(suggestion) {
    return `${suggestion.street} ${suggestion.streetNo}, ${suggestion.zip} ${suggestion.place}`
  }

  renderSuggestion(suggestion) {
    const suggestionText = `${suggestion.street} ${suggestion.streetNo}, ${suggestion.zip} ${suggestion.place}`

    return (
      <span className={'suggestion-content'}>{suggestionText}</span>
    )
  }

  render() {
    const { onSuggestionsUpdateRequested, suggest } = this.props
    const { address } = this.state
    const inputProps = {
      placeholder: 'Adresse',
      value: address,
      type: 'search',
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
                <Col md={1}>
                  <label htmlFor={'addressField'}>Adresse</label>
                </Col>
                <Col md={11}>
                  <Autosuggest id={'addressField'}
                    suggestions={suggest.addressField && suggest.addressField.data ? suggest.addressField.data : []}
                    onSuggestionsUpdateRequested={onSuggestionsUpdateRequested}
                    getSuggestionValue={this.getSuggestionValue}
                    renderSuggestion={this.renderSuggestion}
                    inputProps={inputProps}
                  />
                </Col>
              </Row>
            </Grid>
          </Panel>
          <StorageUnitComponents />
          <EnvironmentRequirementComponent translate={this.props.translate} />

          <Panel>
            <Options
              unit={this.state.sikringBevaring}
              updateSkallsikring={(skallsikring) =>
                this.setState({ sikringBevaring: { ...this.state.sikringBevaring, skallsikring } })}
              updateTyverisikring={(tyverisikring) =>
                this.setState({ sikringBevaring: { ...this.state.sikringBevaring, tyverisikring } })}
              updateBrannsikring={(brannsikring) =>
                this.setState({ sikringBevaring: { ...this.state.sikringBevaring, brannsikring } })}
              updateVannskaderisiko={(vannskaderisiko) =>
                this.setState({ sikringBevaring: { ...this.state.sikringBevaring, vannskaderisiko } })}
              updateRutinerBeredskap={(rutinerBeredskap) =>
                this.setState({ sikringBevaring: { ...this.state.sikringBevaring, rutinerBeredskap } })}
              updateLuftfuktighet={(luftfuktighet) =>
                this.setState({ sikringBevaring: { ...this.state.sikringBevaring, luftfuktighet } })}
              updateLysforhold={(lysforhold) =>
                this.setState({ sikringBevaring: { ...this.state.sikringBevaring, lysforhold } })}
              updateTemperatur={(temperatur) =>
                this.setState({ sikringBevaring: { ...this.state.sikringBevaring, temperatur } })}
              updatePreventivKonservering={(preventivKonservering) =>
                this.setState({ sikringBevaring: { ...this.state.sikringBevaring, preventivKonservering } })}
            />
          </Panel>

        </main>
      </div>
    )
  }
}
