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

import { PageHeader, Panel, Grid, Row, Col, Button, ControlLabel, SplitButton, MenuItem } from 'react-bootstrap'
import FontAwesome from 'react-fontawesome'
import { connect } from 'react-redux'
import Language from '../../components/language'
import DatePicker from 'react-bootstrap-date-picker'
import Autosuggest from 'react-autosuggest'
import { observationTypeDefinitions, defineCommentType,
  defineFromToType, definePestType, defineStatusType } from './observationTypeDefinitions'
import { addObservation, loadObservation } from '../../reducers/observation'
import { actions } from './actions'
import { MusitField } from '../../components/formfields'

// TODO: Bind finished page handling to redux and microservices.
const mapStateToProps = (state) => ({
  translate: (key, markdown) => Language.translate(key, markdown),
  observations: state.observation.data.observations
})

const mapDispatchToProps = (dispatch) => ({
  loadObservation: (id) => {
    dispatch(loadObservation(id))
  },
  onSaveObservation: (data) => {
    dispatch(addObservation(data))
  }
})

@connect(mapStateToProps, mapDispatchToProps)
export default class ObservationView extends React.Component {
  static propTypes = {
    loadObservation: React.PropTypes.func.isRequired,
    translate: React.PropTypes.func.isRequired,
    onSaveObservation: React.PropTypes.func.isRequired,
    observations: React.PropTypes.arrayOf(React.PropTypes.object),
    params: React.PropTypes.object
  }

  constructor(props) {
    super(props)

    const { translate } = props

    this.displayExisting = !!this.props.params.id

    this.observationTypeDefinitions = observationTypeDefinitions(translate, this.actions)
    // TODO: Language binding.
    // TODO: Action binding.

    this.actions = actions(this)

    const label = (key) => translate(`musit.storageUnits.environmentRequirements.${key}`)

    this.observationTypes = {
      lux: defineCommentType(
        'lux', 'Lysforhold',
        label('lightCondition.labelText'),
        label('lightCondition.tooltip'),
        label('renhold.comment'),
        label('renhold.comment'),
        this.actions.changeLuxLeft,
        this.actions.changeLuxRight,
        this.displayExisting
      ),
      gas: defineCommentType(
        'gas', 'Gass',
        label('gas.labelText'),
        label('gas.tooltip'),
        label('renhold.comment'),
        label('renhold.comment'),
        this.actions.changeGasLeft,
        this.actions.changeGasRight,
        this.displayExisting
      ),
      cleaning: defineCommentType(
        'cleaning', 'Renhold',
        label('renhold.labelText'),
        label('renhold.tooltip'),
        label('renhold.comment'), 'Right tooltip',
        this.actions.changeCleaningLeft,
        this.actions.changeCleaningRight,
        this.displayExisting
      ),
      mold: defineCommentType(
        'mold', 'Mugg',
        label('mold.labelText'),
        label('mold.tooltip'),
        label('renhold.comment'),
        label('renhold.comment'),
        this.actions.changeMoldLeft,
        this.actions.changeMoldRight,
        this.displayExisting
      ),
      skallsikring: defineCommentType(
        'skallsikring',
        'Skallsikring', label('skallsikring.labelText'),
        label('skallsikring.tooltip'),
        label('skallsikring.comment'),
        label('skallsikring.comment'),
        this.actions.changeSkallSikringLeft,
        this.actions.changeSkallSikringRight,
        this.displayExisting
      ),
      tyverisikring: defineCommentType(
        'tyverisikring', 'Tyverisikring',
        label('tyverisikring.labelText'),
        label('tyverisikring.tooltip'),
        label('renhold.comment'),
        label('renhold.comment'),
        this.actions.changeTyveriSikringLeft,
        this.actions.changeTyveriSikringRight,
        this.displayExisting
      ),
      brannsikring: defineCommentType(
        'brannsikring', 'Brannsikring',
        label('brannsikring.labelText'),
        label('brannsikring.tooltip'),
        label('brannsikring.comment'),
        label('brannsikring.comment'),
        this.actions.changeBrannSikringLeft,
        this.actions.changeBrannSikringRight,
        this.displayExisting
      ),
      vannskaderisiko: defineCommentType(
        'vannskaderisiko', 'Vannskaderisiko',
        label('vannskaderisiko.labelText'),
        label('vannskaderisiko.tooltip'),
        label('vannskaderisiko.comment'),
        label('vannskaderisiko.comment'),
        this.actions.changeVannskadeRisikoLeft,
        this.actions.changeVannskadeRisikoRight,
        this.displayExisting
      ),
      temperature: defineFromToType(
        'temperature', 'Temperatur',
        label('temperature.labelText'),
        label('temperature.tooltip'),
        label('temperatureTolerance.labelText'),
        label('temperatureTolerance.tooltip'),
        label('temperature.comment'),
        label('temperature.comment'),
        this.actions.changeTempFrom,
        this.actions.changeTempTo,
        this.actions.changeTempComment,
        '', '',
        this.displayExisting
      ),
      rh: defineFromToType(
        'rh', 'Relativ luftfuktighet',
        label('relativeHumidity.labelText'),
        label('relativeHumidity.tooltip'),
        label('relativeHumidityTolerance.labelText'),
        label('relativeHumidityTolerance.tooltip'),
        label('relativeHumidity.comment'),
        label('relativeHumidity.comment'),
        this.actions.changeRHFrom,
        this.actions.changeRHTo,
        this.actions.changeRHComment,
        '', '',
        this.displayExisting
      ),
      hypoxicAir: defineFromToType(
        'hypoxicAir', 'Inert luft',
        label('inertAir.labelText'),
        label('inertAir.tooltip'),
        label('inertAirTolerance.labelText'),
        label('inertAirTolerance.tooltip'),
        label('inertAir.comment'),
        label('inertAir.comment'),
        this.actions.changeHypoxicAirFrom,
        this.actions.changeHypoxicAirTo,
        this.actions.changeHypoxicAirComment,
        'Fra %', 'Til %',
        this.displayExisting
      ),
      alcohol: defineStatusType(
        'alcohol',
        label('alcohol.labelText'),
        label('alcohol.statusLabel'),
        'statusTooltip',
        [label('alcohol.statusItems.dryed'),
        label('alcohol.statusItems.allmostDryed'),
        label('alcohol.statusItems.someDryed'),
        label('alcohol.statusItems.minorDryed'),
        label('alcohol.statusItems.satisfactory')],
        label('alcohol.volume'),
        label('alcohol.tooltip'),
        label('alcohol.comment'),
        label('alcohol.comment'),
        this.actions.changeAlchoholStatus,
        this.actions.changeAlchoholVolume,
        this.actions.changeAlchoholComment,
        this.displayExisting
      ),
      pest: definePestType(
        'pest', 'Skadedyr',
        this.actions.addPest,
        this.actions.changeLifeCycle,
        this.actions.changeCount,
        this.actions.changePestIdentification,
        this.actions.changePestComment,
        this.displayExisting
      )
    }

    this.state = {
      observations: this.displayExisting ? this.props.observations : []
    }

    this.addNewObservation = this.addNewObservation.bind(this)
    this.onChangeDoneBy = this.onChangeDoneBy.bind(this)
    this.onChangeDate = this.onChangeDate.bind(this)
    this.selectType = this.selectType.bind(this)
  }

  componentWillMount() {
    if (this.props.params.id) {
      this.props.loadObservation(this.props.params.id)
    }
  }

  onChangeDoneBy(event, { newValue }) {
    this.updateDoneBy(newValue)
  }

  onChangeDate(v) {
    this.setState({ ...this.state, date: v })
  }

  getDoneBySuggestionValue(suggestion) {
    return `${suggestion.fn}`
  }

  addNewObservation() {
    this.setState({ ...this.state, observations: [...this.state.observations, { type: '' }] })
  }

  selectType(index, observationType) {
    this.setState({
      ...this.state,
      observations: this.state.observations.map((obs, i) => {
        let retVal = obs
        if (index === i) {
          const typeValue = observationType[0]
          const obsType = observationType[1]
          retVal = { ...obs, type: typeValue, data: this.observationTypeDefinitions[obsType.component.viewType].defaultValues }
        }
        return retVal
      })
    })
  }

  updateDoneBy(newValue) {
    this.setState({ ...this.state, doneby: newValue })
  }

  renderDoneBySuggestion(suggestion) {
    const suggestionText = `${suggestion.fn}`
    return (
      <span className={'suggestion-content'}>{suggestionText}</span>
    )
  }

  render() {
    const { translate, onSaveObservation } = this.props
    const {
      addNewObservation,
      observationTypes,
      getDoneBySuggestionValue,
      renderDoneBySuggestion,
      onDoneByUpdateRequested,
      selectType
    } = this

    const { observations } = this.displayExisting ? this.props : this.state

    const doneByProps = {
      id: 'doneByField',
      placeholder: 'Done by',
      value: 'value to be changed',
      type: 'search',
      onChange: this.onChangeDoneBy
    }

    const renderActiveTypes = (items) => {
      return items.map((observation, index) => {
        // TODO: Check for new type, how to handle STEIN: DONE (?)
        let subBlock = ''
        if (observation.type && observation.type.length > 0) {
          const observationType = this.observationTypes[observation.type]
          const observationTypeDef = this.observationTypeDefinitions[observationType.component.viewType]

          // TODO: Draw type field. And action to change type, with state reset and default variables. Stein:DONE
          subBlock = observationTypeDef.render(index, {
            ...observationType.component.props,
            ...observationTypeDef.props,
            ...observation.data
          })
        }
        const menuItems = Object.entries(observationTypes).map((obsType, row) => (
          <MenuItem
            key={row}
            disabled={this.state.observations.findIndex((e) => (e.type === obsType[0])) >= 0}
            eventKey={obsType}
          >
            {obsType[1].label}
          </MenuItem>
        ))
        return (
          <Row key={index}>
            <hr />
            <Col sm={10} smOffset={1}>
              <SplitButton
                id={`new_${index}`}
                title={observation.type ? observationTypes[observation.type].label : null || 'Vennligst velg...'}
                pullRight
                disabled={this.displayExisting}
                onSelect={(observationType) => selectType(index, observationType)}
              >
                {menuItems}
              </SplitButton>
              <h1 />
              {subBlock}
            </Col>
          </Row>
        )
      })
    }

    return (
      <div>
        <main>
          <Panel>
            <Grid>
              <Row>
                <Col style={{ textAlign: 'center' }}>
                  <PageHeader>
                    {this.displayExisting ? translate('musit.observation.viewObservationHeader') :
                    translate('musit.observation.newObservationHeader')}
                  </PageHeader>
                </Col>
              </Row>
              <Row>
                <Col xs={12} sm={5} smOffset={1}>
                  <ControlLabel>{translate('musit.observation.date')}</ControlLabel>
                  <DatePicker
                    value={this.state.date}
                    disabled={this.displayExisting}
                    onChange={this.onChangeDate}
                  />
                </Col>
                <Col xs={12} sm={5}>
                  <ControlLabel>{translate('musit.observation.doneBy')}</ControlLabel>
                  <Autosuggest
                    suggestions={[{ fn: 'test1' }, { fn: 'test2' }]}
                    disabled={this.displayExisting}
                    onSuggestionsUpdateRequested={onDoneByUpdateRequested}
                    getSuggestionValue={getDoneBySuggestionValue}
                    renderSuggestion={renderDoneBySuggestion}
                    inputProps={doneByProps}
                    shouldRenderSuggestions={(v) => v !== 'undefined'}
                  />
                </Col>
              </Row>
              {this.displayExisting ?
                <Row>
                  <Col sm={5} smOffset={1}>
                    <ControlLabel>{translate('musit.texts.dateRegistered')}</ControlLabel>
                    <br />
                    <DatePicker
                      dateFormat="DD.MM.YYYY"
                      value={this.state.date}
                      disabled={this.displayExisting}
                    />
                  </Col>
                  <Col sm={5} >
                    <ControlLabel>{translate('musit.texts.registeredBy')}</ControlLabel>
                    <br />
                    <MusitField id="registeredBy" value="Change it" validate="text" disabled={Boolean(true)} />
                  </Col>
                </Row>
              : ''}
              <h1 />
              {renderActiveTypes(observations)}
              <Row>
                <h1 />
                <hr />
                <Col sm={5} smOffset={1}>
                  <Button
                    onClick={() => addNewObservation()}
                    disabled={this.displayExisting}
                  >
                    <FontAwesome name="plus-circle" /> {translate('musit.observation.newButtonLabel')}
                  </Button>
                </Col>
              </Row>
              <Row>
                <h1 />
                <h1 />
                <hr />
                <Col sm={10} smOffset={1} className="text-center">
                  <Button
                    onClick={() => onSaveObservation(this.state)}
                    disabled={this.displayExisting}
                  >
                    {translate('musit.texts.save')}
                  </Button>
                  <Button
                    onClick={() => addNewObservation()}
                    disabled={this.displayExisting}
                  >
                    {translate('musit.texts.cancel')}
                  </Button>
                </Col>
              </Row>
            </Grid>
          </Panel>
        </main>
      </div>
    )
  }
}
