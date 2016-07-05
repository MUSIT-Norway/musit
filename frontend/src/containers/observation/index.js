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

import { Panel, Grid, Row, Col, FormGroup, Button, ControlLabel, SplitButton, MenuItem } from 'react-bootstrap'
import FontAwesome from 'react-fontawesome'
import { connect } from 'react-redux'
import Language from '../../components/language'
import DatePicker from 'react-datepicker'
require('react-datepicker/dist/react-datepicker.css');
import Autosuggest from 'react-autosuggest'
import { observationTypeDefinitions, defineCommentType,
  defineFromToType, definePestType, defineStatusType } from './observationTypeDefinitions'

// TODO: Bind finished page handling to redux and microservices.
const mapStateToProps = () => ({
  translate: (key, markdown) => Language.translate(key, markdown)
})

@connect(mapStateToProps)
export default class ObservationView extends React.Component {
  static propTypes = {
    translate: React.PropTypes.func.isRequired
  }

  constructor(props) {
    super(props)
    const { translate } = props

    this.observationTypeDefinitions = observationTypeDefinitions(translate, this.addPest)
    // TODO: Language binding.
    // TODO: Action binding.
    this.observationTypes = {
      lux: defineCommentType('lux', 'Lysforhold', 'Left label', 'Left tooltip', 'Right label', 'Right tooltip'),
      gas: defineCommentType('gas', 'Gass', 'Left label', 'Left tooltip', 'Right label', 'Right tooltip'),
      cleaning: defineCommentType('cleaning', 'Renhold', 'Left label', 'Left tooltip', 'Right label', 'Right tooltip'),
      mold: defineCommentType('mold', 'Mugg', 'Left label', 'Left tooltip', 'Right label', 'Right tooltip'),
      skallsikring: defineCommentType('skallsikring',
        'Skallsikring', 'Left label', 'Left tooltip', 'Right label', 'Right tooltip'),
      tyverisikring: defineCommentType('tyverisikring',
        'Tyverisikring', 'Left label', 'Left tooltip', 'Right label', 'Right tooltip'),
      brannsikring: defineCommentType('brannsikring',
        'Brannsikring', 'Left label', 'Left tooltip', 'Right label', 'Right tooltip'),
      vannskaderisiko: defineCommentType('vannskaderisiko',
        'Vannskaderisiko', 'Left label', 'Left tooltip', 'Right label', 'Right tooltip'),
      temperature: defineFromToType('temperature', 'Temperatur', 'from label',
        'From tooltip', 'from label', 'To tooltip', 'Comment label', 'Comment tooltip'),
      rh: defineFromToType('rh', 'Relativ luftfuktighet', 'from label', 'From tooltip',
        'from label', 'To tooltip', 'Comment label', 'Comment tooltip'),
      hypoxicAir: defineFromToType('hypoxicAir', 'Inertluft', 'from label',
        'From tooltip', 'from label', 'To tooltip', 'Comment label', 'Comment tooltip'),
      alcohol: defineStatusType('alcohol', 'Sprit', 'Status label', 'statusTooltip',
        ['Uttørket', 'Nesten uttørket', 'Noe uttørket', 'Litt uttørket', 'Tilfredstillende'],
        'volumeLabel', 'volumeTooltip', 'commentLabel', 'commentTooltip'),
      pest: definePestType('pest', 'Skadedyr')
    }

    this.state = {
      observations: [
        /* {
          type: 'pest',
          data: {
            observations: [],
            identificationValue: '',
            commentsValue: ''
          }
        },
        {
          type: 'hypoxicAir',
          data: {
            fromValue: '',
            toValue: '',
            commentValue: ''
          }
        },
        {
          type: 'cleaning',
          data: {
            leftValue: '',
            rightValue: ''
          }
        },
        {
          type: 'alcohol',
          data: {
            statusValue: '',
            volumeValue: '',
            commentValue: ''
          }
        }*/
      ]
    }
    this.addNewObservation = this.addNewObservation.bind(this)
    this.onChangeDoneBy = this.onChangeDoneBy.bind(this)
    this.addPest = this.addPest.bind(this)
    this.selectType = this.selectType.bind(this)
  }

  onChangeDoneBy(event, { newValue }) {
    this.updateDoneBy(newValue)
  }

  getDoneBySuggestionValue(suggestion) {
    return `${suggestion.fn}`
  }

  addPest() {
    // this.setState({ ...this.state, pestObservations: [...this.state.pestObservations, { lifeCycle: '', count: 0 }] })
    console.log('add pest')
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
          console.log(retVal)
          console.log(observationType)
        }
        return retVal
      })
    })
  }

  updateDoneBy(newValue) {
    console.log(newValue)
  }

  renderDoneBySuggestion(suggestion) {
    const suggestionText = `${suggestion.fn}`
    return (
      <span className={'suggestion-content'}>{suggestionText}</span>
    )
  }

  render() {
    const { translate } = this.props
    const {
      addNewObservation,
      observationTypes,
      getDoneBySuggestionValue,
      renderDoneBySuggestion,
      onDoneByUpdateRequested,
      selectType
    } = this
    const { observations } = this.state

    const doneByProps = {
      id: 'addressField',
      placeholder: 'addresse',
      value: 'value to be changed',
      type: 'search',
      onChange: this.onChangeDoneBy
    }

    const renderActiveTypes = (items) => {
      const block = items.map((observation, index) => {
        // TODO: Check for new type, how to handle
        let subBlock = ''
        if (observation.type && observation.type.length > 0) {
          const observationType = this.observationTypes[observation.type]
          const observationTypeDef = this.observationTypeDefinitions[observationType.component.viewType]

          // TODO: Draw type field. And action to change type, with state reset and default variables.
          subBlock = observationTypeDef.render(index, {
            ...observationType.component.props,
            ...observationTypeDef.props,
            ...observation.data
          })
        }
        const menuItems = Object.entries(observationTypes).map((obsType, row) => (
          <MenuItem
            key={row}
            disabled={this.state.observations.find((e) => (e.type === obsType[0]))}
            eventKey={obsType}
          >
            {obsType[1].label}
          </MenuItem>
        ))
        return (
          <Row key={index}>
            <Col sm={10} smOffset={1}>
              <SplitButton
                id={`new_${index}`}
                title={observation.type || 'Vennligst velg...'}
                pullRight
                onSelect={(observationType) => selectType(index, observationType)}
              >
                {menuItems}
              </SplitButton>
              {subBlock}
            </Col>
          </Row>
        )
      })
      return block
    }

    return (
      <div>
        <main>
          <Panel>
            <Grid>
              <Row>
                <Col sm={10} smOffset={1}>
                  <FormGroup>
                    <Col xs={12} sm={6}>
                      <ControlLabel>{translate('musit.texts.date')}</ControlLabel>
                      <DatePicker />
                    </Col>
                    <Col xs={12} sm={6}>
                      <ControlLabel>{translate('musit.texts.doneBy')}</ControlLabel>
                      <Autosuggest
                        suggestions={[{ fn: 'test1' }, { fn: 'test2' }]}
                        onSuggestionsUpdateRequested={onDoneByUpdateRequested}
                        getSuggestionValue={getDoneBySuggestionValue}
                        renderSuggestion={renderDoneBySuggestion}
                        inputProps={doneByProps}
                        shouldRenderSuggestions={(v) => v !== 'undefined'}
                      />
                    </Col>
                  </FormGroup>
                </Col>
              </Row>
              {renderActiveTypes(observations)}
              <Row>
                <Col sm={10} smOffset={1} className="text-center">
                  <Button onClick={() => addNewObservation()}>
                    <FontAwesome name="plus-circle" /> {translate('musit.observation.newButtonLabel')}
                  </Button>
                </Col>
              </Row>
              <Row>
                <Col sm={10} smOffset={1} className="text-center">
                  <Button onClick={() => addNewObservation()}>
                    {translate('musit.texts.save')}
                  </Button>
                  <Button onClick={() => addNewObservation()}>
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
