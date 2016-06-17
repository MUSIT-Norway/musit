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
 import React, { Component, PropTypes } from 'react'
 import Options from '../../components/storageunits/EnvironmentOptions'
 import { connect } from 'react-redux';
 import StorageUnitComponents from '../../components/storageunits/StorageUnitComponent'
 import { insert as insertStorageUnitContainer } from '../../reducers/storageunit-container';
 import { load } from '../../reducers/storageunit-container';
 import { ButtonToolbar, Button, Grid, Row } from 'react-bootstrap'
 import Language from '../../components/language'
 import EnvironmentRequirementComponent from '../../components/storageunits/EnvironmentRequirementComponent'
 import { suggestAddress, clearSuggest } from '../../reducers/suggest'

 const mapStateToProps = (state) => {
   console.log(state.storageUnit)
   return {
     storageUnit: (state.storageUnit && state.storageUnit.data) ? state.storageUnit.data : {},
     translate: (key, markdown) => Language.translate(key, markdown),
     suggest: state.suggest
   }
 }

 const mapDispatchToProps = (dispatch) => {
   return {
     onLagreClick: (data) => {
       dispatch(insertStorageUnitContainer(data))
     },
     loadStorageUnit: (id) => {
       dispatch(load(id))
     },
     onAddressSuggestionsUpdateRequested: ({ value, reason }) => {
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

 export default class StorageUnitContainer extends Component {
   static propTypes = {
     storageUnit: PropTypes.object.isRequired,
     id: PropTypes.number,
     store: PropTypes.object.isRequired,
     loadStorageUnit: PropTypes.func.isRequired,
     onAddressSuggestionsUpdateRequested: PropTypes.func.isRequired,
     suggest: React.PropTypes.array.isRequired,
     params: PropTypes.Object,
     onLagreClick: PropTypes.func.isRequired,
     updateStorageUnit: PropTypes.func.isRequired,
     translate: PropTypes.func.isRequired,
   }


   componentWillMount() {
     this.props.loadStorageUnit(this.props.params.id)
   }

   updateStorageUnit(data, key, value) {
     const newData = Object.assign({}, data);
     console.log(key)
     newData[key] = value && value !== '' ? value : null
     this.setState({ storageUnit: newData })
   }

   render() {
     const data = (this.state && this.state.storageUnit) ? this.state.storageUnit : this.props.storageUnit;
     return (
      <div>
        <main>
          <Grid>
            <Row styleClass="row-centered">
              <ButtonToolbar>
                <Button bsStyle="primary" onClick= {() => this.props.onLagreClick(this.state.storageUnit)} >Lagre</Button>
                <Button>Cancel</Button>
              </ButtonToolbar>
            </Row>
          </Grid>
          <StorageUnitComponents
            unit= { data }
            updateType = {(storageType) =>
              this.updateStorageUnit(data, 'storageType', storageType)}
            updateName = {(storageUnitName) =>
              this.updateStorageUnit(data, 'storageUnitName', storageUnitName)}
            updateAreal1 = {(area) =>
              this.updateStorageUnit(data, 'area', area ? parseFloat(area) : area)}
            updateAreal2 = {(areal2) =>
              this.updateStorageUnit(data, 'area2', areal2 ? parseFloat(areal2) : areal2)}
            updateHeight1 = {(height) =>
              this.updateStorageUnit(data, 'height', height ? parseFloat(height) : height)}
            updateHeight2 = {(height2) =>
              this.updateStorageUnit(data, 'height2', height2 ? parseFloat(height2) : height2)}
            updateAddress = {(address) =>
              this.updateStorageUnit(data, 'address', address)}
            onAddressSuggestionsUpdateRequested = { this.props.onAddressSuggestionsUpdateRequested }
            suggest= { this.props.suggest }
          />
          <EnvironmentRequirementComponent
            translate={ this.props.translate }
          />
          <Options
            unit = { data }
             // Disse må fikses (Mappe verdi av sikring fra bool -> {0,1})
            updateSkallsikring={(sikringSkallsikring) =>
              this.updateStorageUnit(data, 'sikringSkallsikring', sikringSkallsikring)}
            updateTyverisikring={(sikringTyverisikring) =>
              this.updateStorageUnit(data, 'sikringTyverisikring', sikringTyverisikring)}
            updateBrannsikring={(sikringBrannsikring) =>
              this.updateStorageUnit(data, 'sikringBrannsikring', sikringBrannsikring)}
            updateVannskaderisiko={(sikringVannskaderisiko) =>
              this.updateStorageUnit(data, 'sikringVannskaderisiko', sikringVannskaderisiko)}
            updateRutinerBeredskap={(sikringRutineOgBeredskap) =>
              this.updateStorageUnit(data, 'sikringRutineOgBeredskap', sikringRutineOgBeredskap)}
            updateLuftfuktighet={(bevarLuftfuktOgTemp) =>
              this.updateStorageUnit(data, 'bevarLuftfuktOgTemp', bevarLuftfuktOgTemp)}
            updateLysforhold={(bevarLysforhold) =>
              this.updateStorageUnit(data, 'bevarLysforhold', bevarLysforhold)}
            updateTemperatur={(temperatur) =>
              this.updateStorageUnit(data, 'temperatur', temperatur)}
            updatePreventivKonservering={(bevarPrevantKons) =>
              this.updateStorageUnit(data, 'bevarPrevantKons', bevarPrevantKons)}
          />
        </main>
      </div>
      );
   }
}
