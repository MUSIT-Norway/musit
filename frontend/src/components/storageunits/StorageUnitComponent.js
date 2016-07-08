/**
 * Created by steinaol on 5/27/16.
 */

import React, { Component } from 'react';
import { MusitDropDownField as MusitDropDown, MusitField as TextField } from '../../components/formfields'
import { Panel, Form, Grid, Row, Col, FormGroup } from 'react-bootstrap'
import Autosuggest from 'react-autosuggest'

export default class StorageUnitComponent extends Component {

  static propTypes = {
    unit: React.PropTypes.shape({
      name: React.PropTypes.string,
      area: React.PropTypes.string,
      areaTo: React.PropTypes.number,
      height: React.PropTypes.number,
      heightTo: React.PropTypes.number,
      type: React.PropTypes.string,
      address: React.PropTypes.string }
    ),
    updateType: React.PropTypes.func.isRequired,
    updateName: React.PropTypes.func.isRequired,
    updateHeight1: React.PropTypes.func.isRequired,
    updateHeight2: React.PropTypes.func.isRequired,
    updateAreal1: React.PropTypes.func.isRequired,
    updateAreal2: React.PropTypes.func.isRequired,
    updateAddress: React.PropTypes.func.isRequired,
    suggest: React.PropTypes.array.isRequired,
    onAddressSuggestionsUpdateRequested: React.PropTypes.func.isRequired,
    translate: React.PropTypes.func.isRequired,
  }

  constructor(props) {
    super(props)

    this.areal = {
      controlId: 'areal1',
      controlId2: 'areal2',
      labelText: 'Areal (fra - til)',
      tooltip: 'Areal (fra - til)',
      validate: 'number',
      placeHolderText: 'enter areal 1 here',
      placeHolderText2: 'enter areal 2 here',
      valueText: () => this.props.unit.area,
      valueText2: () => this.props.unit.areaTo,
      onChange1: (area) => this.props.updateAreal1(area),
      onChange2: (areal2) => this.props.updateAreal2(areal2)
    }
    this.hoyde = {
      controlId: 'hoyde1',
      controlId2: 'hoyde2',
      labelText: 'Høyde(fra - til)',
      tooltip: 'Høyde (fra - til)',
      validate: 'number',
      placeHolderText: 'enter høyde 1 here',
      placeHolderText2: 'enter høyde 2 here',
      valueText: () => this.props.unit.height,
      valueText2: () => this.props.unit.heightTo,
      onChange1: (height) => this.props.updateHeight1(height),
      onChange2: (height2) => this.props.updateHeight2(height2)
    }

    this.type = {
      controlId: 'type',
      labelText: 'Type',
      items: ['StorageUnit', 'Room', 'Building', 'Organization'],
      validate: 'text',
      tooltip: 'Type lagringsenhet',
      placeHolder: 'velg type here',
      valueText: () => this.props.unit.type,
      onChange: (storageType) => this.props.updateType(storageType)
    }
    this.name = {
      controlId: 'name',
      labelText: 'Navn',
      tooltip: 'Navn',
      placeHolderText: 'enter name here',
      validate: 'text',
      valueText: () => this.props.unit.name,
      onChange: (storageUnitName) => this.props.updateName(storageUnitName)
    }
    this.onAddressChange = this.onAddressChange.bind(this)
  }

  onAddressChange(event, { newValue }) {
    this.props.updateAddress(newValue)
  }

  getAddressSuggestionValue(suggestion) {
    return `${suggestion.street} ${suggestion.streetNo}, ${suggestion.zip} ${suggestion.place}`
  }

  renderAddressSuggestion(suggestion) {
    const suggestionText = `${suggestion.street} ${suggestion.streetNo}, ${suggestion.zip} ${suggestion.place}`
    return (
      <span className={'suggestion-content'}>{suggestionText}</span>
    )
  }

  render() {
    const renderFieldBlock = (bindValue, fieldProps, label) => (
      <FormGroup>
        <label className="col-sm-3 control-label" htmlFor="comments2">{label}</label>
        <div class="col-sm-9" is="null">
          <TextField {...fieldProps} value={bindValue} />
        </div>
      </FormGroup>
    )
    const inputAddressProps = {
      id: 'addressField',
      placeholder: 'addresse',
      value: this.props.unit.address,
      type: 'search',
      onChange: this.onAddressChange
    }
    const {
      onAddressSuggestionsUpdateRequested,
      suggest } = this.props
    const { addressField } = suggest;

    const suggestions = addressField && addressField.data ? addressField.data : [];

    const addressBlock = (
      <Row>
        <Col md={3}>
          <label htmlFor={'addressField'}>Adresse</label>
        </Col>
        <Col md={9}>
          <Autosuggest
            suggestions={suggestions}
            onSuggestionsUpdateRequested={onAddressSuggestionsUpdateRequested}
            getSuggestionValue={this.getAddressSuggestionValue}
            renderSuggestion={this.renderAddressSuggestion}
            inputProps={inputAddressProps}
            shouldRenderSuggestions={(v) => v !== 'undefined'}
          />
        </Col>
      </Row>
    )

    return (
      <div>
        <main>
          <Panel>
            <Grid>
              <Row className="row-centered">
                <Col md={6}>
                  <form className="form-horizontal">
                    <form className="form-group">
                      <label className="col-sm-3 control-label" htmlFor="storageUnitType">
                        {this.type.labelText}</label>
                      <div class="col-sm-5" is="null">
                        <MusitDropDown
                          {...this.type}
                          id={this.type.controlId}
                          value={this.type.valueText()}
                          translate={this.props.translate}
                          translateKey={'musit.storageUnits.storageType.items.'}
                        />
                      </div>
                    </form>
                  </form>
                </Col>
                <Col md={6}>
                  <Form horizontal>
                    {renderFieldBlock(this.name.valueText(), this.name, this.name.labelText)}
                  </Form>
                </Col>
              </Row>
              <Row styleClass="row-centered">
                <Col md={6}>
                  <form className="form-horizontal">
                    <div className="form-group">
                      <label className="col-sm-3 control-label" htmlFor="comments2">
                        {this.areal.labelText}</label>
                      <div class="col-sm-5" is="null">
                        <TextField
                          id={this.areal.controlId}
                          value={this.areal.valueText()}
                          onChange={this.areal.onChange1}
                          placeHolder={this.areal.placeHolderText}
                          validate={this.areal.validate}
                        />
                      </div>
                      <div class="col-sm-4" is="null">
                        <TextField
                          id={this.areal.controlId2}
                          value={this.areal.valueText2()}
                          onChange={this.areal.onChange2}
                          placeHolder={this.areal.placeHolderText2}
                          validate={this.areal.validate}
                        />
                      </div>
                    </div>
                  </form>
                </Col>
                <Col md={6}>
                  <Form horizontal>
                    <div className="form-group">
                      <label className="col-sm-3 control-label" htmlFor="controlId">
                        {this.hoyde.labelText}</label>
                      <div class="col-sm-5" is="null">
                        <TextField
                          id={this.hoyde.controlId}
                          value={this.hoyde.valueText()}
                          onChange={this.hoyde.onChange1}
                          placeHolder={this.hoyde.placeHolderText}
                          validate={this.areal.validate}
                        />
                      </div>
                      <div class="col-sm-4" is="null">
                        <TextField
                          id={this.hoyde.controlId2}
                          value={this.hoyde.valueText2()}
                          onChange={this.hoyde.onChange2}
                          placeHolder={this.hoyde.placeHolderText2}
                          validate={this.areal.validate}
                        />
                      </div>
                    </div>
                  </Form>
                </Col>
              </Row >
              <Row>
                <Col md={6}>
                  {this.props.unit.type === 'building' ? addressBlock : null}
                </Col>
              </Row>
            </Grid>
          </Panel>
        </main>
      </div>
    );
  }
}
