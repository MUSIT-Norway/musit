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

 const mapStateToProps = (state) => {
   return {
     storageUnit: (state.storageUnit && state.storageUnit.data) ? { ...state.storageUnit.data } : {},
     translate: (key, markdown) => Language.translate(key, markdown)
   }
 }

 const mapDispatchToProps = (dispatch) => {
   return {
     onLagreClick: (data) => {
       dispatch(insertStorageUnitContainer(data))
     },
     loadStorageUnit: (id) => {
       dispatch(load(id))
     }
   }
 }

@connect(mapStateToProps, mapDispatchToProps)

 export default class StorageUnitContainer extends Component {
   static propTypes = {
     storageUnit: PropTypes.object.isRequired,
     store: PropTypes.object.isRequired,
     loadStorageUnit: PropTypes.func.isRequired,
     onLagreClick: PropTypes.func.isRequired,
     updateStorageUnit: PropTypes.func.isRequired,
     translate: PropTypes.func.isRequired,
   }

   constructor(props) {
     super(props)
     this.state = { storageUnit: this.props.storageUnit }
   }

   updateStorageUnit(data, key, value) {
     data[key] = value
     data.storageType = 'storageunit'
     // console.log(value)
     const b = data
     this.setState({ ...b })
   }


   render() {
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
            unit= { this.state.storageUnit }
            updateType = {(storageType) =>
              this.state.updateStorageUnit(this.state.storageUnit, 'storageType', storageType)}
            updateName= {(storageUnitName) =>
              this.updateStorageUnit(this.state.storageUnit, 'storageUnitName', storageUnitName)}
            updateAreal1= {(area) =>
              this.updateStorageUnit(this.state.storageUnit, 'area', area ? parseFloat(area) : area)}
            updateAreal2= {(areal2) =>
              this.updateStorageUnit(this.state.storageUnit, 'area2', areal2 ? parseFloat(areal2) : areal2)}
            updateHeight1= {(height) =>
              this.updateStorageUnit(this.state.storageUnit, 'height', height ? parseFloat(height) : height)}
            updateHeight2= {(height2) =>
              this.updateStorageUnit(this.state.storageUnit, 'height2', height2 ? parseFloat(height2) : height2)}
          />
          <EnvironmentRequirementComponent translate={this.props.translate} />
          <Options
            unit = { this.state.storageUnit }
             // Disse må fikses (Mappe verdi av sikring fra bool -> {0,1})
            updateSkallsikring={(sikringSkallsikring) =>
              this.updateStorageUnit(this.state.storageUnit, 'sikringSkallsikring', sikringSkallsikring)}
            updateTyverisikring={(sikringTyverisikring) =>
              this.updateStorageUnit(this.state.storageUnit, 'sikringTyverisikring', sikringTyverisikring)}
            updateBrannsikring={(sikringBrannsikring) =>
              this.updateStorageUnit(this.state.storageUnit, 'sikringBrannsikring', sikringBrannsikring)}
            updateVannskaderisiko={(sikringVannskaderisiko) =>
              this.updateStorageUnit(this.state.storageUnit, 'sikringVannskaderisiko', sikringVannskaderisiko)}
            updateRutinerBeredskap={(sikringRutineOgBeredskap) =>
              this.updateStorageUnit(this.state.storageUnit, 'sikringRutineOgBeredskap', sikringRutineOgBeredskap)}
            updateLuftfuktighet={(bevarLuftfuktOgTemp) =>
              this.updateStorageUnit(this.state.storageUnit, 'bevarLuftfuktOgTemp', bevarLuftfuktOgTemp)}
            updateLysforhold={(bevarLysforhold) =>
              this.updateStorageUnit(this.state.storageUnit, 'bevarLysforhold', bevarLysforhold)}
            updateTemperatur={(temperatur) =>
              this.updateStorageUnit(this.state.storageUnit, 'temperatur', temperatur)}
            updatePreventivKonservering={(bevarPrevantKons) =>
              this.updateStorageUnit(this.state.storageUnit, 'bevarPrevantKons', bevarPrevantKons)}
          />
        </main>
      </div>
      );
   }
}
