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
import Options from '../../components/storageunits/EnvironmentOptions'
import { Button, Panel, Grid, Row, Col } from 'react-bootstrap'
import { connect } from 'react-redux'
import Autosuggest from 'react-autosuggest'
import { suggestAddress, suggestCompany, clearSuggest } from '../../reducers/suggest'
import { createOrganization } from '../../reducers/organization'
import { OrganizationPopupContainer } from '../../components/organization'
import Language from '../../components/language'
import EnvironmentRequirementComponent from '../../components/storageunits/EnvironmentRequirementComponent'

const mapStateToProps = (state) => ({
  suggest: state.suggest,
  user: state.auth.user,
  translate: (key, markdown) => Language.translate(key, markdown)
})

const mapDispatchToProps = (dispatch) => ({
  onAddressSuggestionsUpdateRequested: ({ value, reason }) => {
    // Should only autosuggest on typing if you have more then 3 characters
    if (reason && (reason === 'type') && value && (value.length >= 3)) {
      dispatch(suggestAddress('addressField', value))
    } else {
      dispatch(clearSuggest('addressField'))
    }
  },
  onOrganizationSuggestionsUpdateRequested: ({ value, reason }) => {
    // Should only autosuggest on typing if you have more then 3 characters
    if (reason && (reason === 'type') && value && (value.length >= 1)) {
      dispatch(suggestCompany('organizationField', value))
    } else {
      dispatch(clearSuggest('organizationField'))
    }
  },
  dispatchCreateOrganization: (organization) => {
    console.log(organization)
    if (organization && organization.fn && organization.fn.length > 0) {
      dispatch(createOrganization(organization))
    }
  }
})

@connect(mapStateToProps, mapDispatchToProps)
export default class ExampleView extends React.Component {
  static propTypes = {
    translate: React.PropTypes.func.isRequired,
    user: React.PropTypes.object,
    onAddressSuggestionsUpdateRequested: React.PropTypes.func.isRequired,
    onOrganizationSuggestionsUpdateRequested: React.PropTypes.func.isRequired,
    dispatchCreateOrganization: React.PropTypes.func.isRequired,
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
      organizationData: {
        fn: '',
        nickname: '',
        tel: '',
        web: ''
      },
      address: '',
      organization: '',
      showCreate: false
    }

    this.areal = {
      controlId: 'areal',
      labelText: 'Areal',
      tooltip: 'Areal',
      valueType: 'text',
      placeHolderText: 'enter areal here',
      valueText: () => this.state.unit.areal,
      validationState: () => ExampleView.validateNumber(this.state.unit.areal),
      onChange: (areal) => this.setState({ unit: { ...this.state.unit, areal } })
    }

    this.fields = [
      {
        controlId: 'name',
        labelText: 'Name',
        tooltip: 'Name',
        valueType: 'text',
        placeHolderText: 'enter name here',
        valueText: () => this.state.unit.name,
        validationState: () => ExampleView.validateString(this.state.unit.name),
        onChange: (name) => this.setState({ unit: { ...this.state.unit, name } })
      },
      {
        controlId: 'description',
        labelText: 'Description',
        tooltip: 'Description',
        placeHolderText: 'enter description here',
        valueType: 'text',
        valueText: () => this.state.unit.description,
        validationState: () => ExampleView.validateString(this.state.unit.description),
        onChange: (description) => this.setState({ unit: { ...this.state.unit, description } })
      },
      {
        controlId: 'note',
        labelText: 'Note',
        tooltip: 'Note',
        valueType: 'text',
        placeHolderText: 'enter note here',
        valueText: () => this.state.unit.note,
        validationState: () => ExampleView.validateString(this.state.unit.note),
        onChange: (note) => this.setState({ unit: { ...this.state.unit, note } })
      }
    ]
    this.onAddressChange = this.onAddressChange.bind(this)
    this.onOrganizationChange = this.onOrganizationChange.bind(this)
    this.openOrganizationCreate = this.openOrganizationCreate.bind(this)
    this.closeOrganizationCreate = this.closeOrganizationCreate.bind(this)
    this.onOrganizationSave = this.onOrganizationSave.bind(this)
  }

  onAddressChange(event, { newValue }) {
    this.setState({
      address: newValue
    })
  }

  onOrganizationChange(event, { newValue }) {
    this.setState({
      organization: newValue
    })
  }

  onOrganizationSave(event) {
    console.log(event)
    this.props.dispatchCreateOrganization({ ...this.state.organizationData })
    this.closeOrganizationCreate()
  }

  getAddressSuggestionValue(suggestion) {
    return `${suggestion.street} ${suggestion.streetNo}, ${suggestion.zip} ${suggestion.place}`
  }

  getOrganizationSuggestionValue(suggestion) {
    return `[${suggestion.nickname}] ${suggestion.fn}`
  }

  closeOrganizationCreate() {
    this.setState({ showCreate: false })
  }

  openOrganizationCreate() {
    this.setState({
      organizationData: {
        fn: '',
        nickname: '',
        tel: '',
        web: ''
      }
    })
    this.setState({ showCreate: true })
  }

  renderAddressSuggestion(suggestion) {
    const suggestionText = `${suggestion.street} ${suggestion.streetNo}, ${suggestion.zip} ${suggestion.place}`

    return (
      <span className={'suggestion-content'}>{suggestionText}</span>
    )
  }

  renderOrganizationSuggestion(suggestion) {
    const suggestionText = `[${suggestion.nickname}] ${suggestion.fn}`

    return (
      <span className={'suggestion-content'}>{suggestionText}</span>
    )
  }

  render() {
    const {
      onAddressSuggestionsUpdateRequested,
      onOrganizationSuggestionsUpdateRequested,
      suggest } = this.props
    const { address, organization, showCreate, organizationData } = this.state

    const inputAddressProps = {
      placeholder: 'Adresse',
      value: address,
      type: 'search',
      onChange: this.onAddressChange
    }
    const inputOrganizationProps = {
      placeholder: 'Organisasjon',
      value: organization,
      type: 'search',
      onChange: this.onOrganizationChange
    }
    const organizationSuggestions =
      suggest.organizationField && suggest.organizationField.data ? suggest.organizationField.data : []

    return (
      <div>
        <main>
          <OrganizationPopupContainer show={showCreate}
            org={organizationData}
            updateFN={(fn) => this.setState({ organizationData: { ...organizationData, fn } })}
            updateNickname={(nickname) => this.setState({ organizationData: { ...organizationData, nickname } })}
            updateTel={(tel) => this.setState({ organizationData: { ...organizationData, tel } })}
            updateWeb={(web) => this.setState({ organizationData: { ...organizationData, web } })}
            onClose={this.closeOrganizationCreate}
            onSave={this.onOrganizationSave}
          />
          <Panel>
            <Grid>
              <Row>
                <Col md={2}>
                  <label htmlFor={'addressField'}>Adresse</label>
                </Col>
                <Col md={4}>
                  <Autosuggest
                    id={'addressField'}
                    suggestions={suggest.addressField && suggest.addressField.data ? suggest.addressField.data : []}
                    onSuggestionsUpdateRequested={onAddressSuggestionsUpdateRequested}
                    getSuggestionValue={this.getAddressSuggestionValue}
                    renderSuggestion={this.renderAddressSuggestion}
                    inputProps={inputAddressProps}
                  />
                </Col>
                <Col md={2}>
                  <label htmlFor={'organizationField'}>Organisasjon</label>
                </Col>
                <Col md={3}>
                  <Autosuggest
                    id={'organizationField'}
                    suggestions={organizationSuggestions}
                    onSuggestionsUpdateRequested={onOrganizationSuggestionsUpdateRequested}
                    getSuggestionValue={this.getOrganizationSuggestionValue}
                    renderSuggestion={this.renderOrganizationSuggestion}
                    inputProps={inputOrganizationProps}
                  />
                </Col>
                <Col md={1}>
                  <Button bsSize={'small'} onClick={this.openOrganizationCreate}>Opprett</Button>
                </Col>
              </Row>
            </Grid>
          </Panel>
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
