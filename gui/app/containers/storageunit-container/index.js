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
 import { asyncConnect } from 'redux-async-connect';
 import StorageUnitComponents from '../../components/storageunits/StorageUnitComponent'
 import { insert as insertStorageUnitContainer } from '../../reducers/storageunit-container';
 import { load, isLoaded } from '../../reducers/storageunit-container';
 import { ButtonToolbar, Button, Grid, Row } from 'react-bootstrap'

 const mapStateToProps = (state) => {
   return {
     storageUnit: (state.storageUnit && state.storageUnit.data) ? { ...state.storageUnit.data } : {}
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

 @asyncConnect([{
   promise: ({ store: { dispatch, getState } }) => {
     const promises = [];
     if (!isLoaded(getState())) {
       promises.push(dispatch(load(1)))
     }
     return Promise.all(promises);
   }
 }])


@connect(mapStateToProps, mapDispatchToProps)

 export default class StorageUnitContainer extends Component {
   static propTypes = {
     storageUnit: PropTypes.object.isRequired,
     store: PropTypes.object.isRequired,
     loadStorageUnit: PropTypes.func.isRequired,
     onLagreClick: PropTypes.func.isRequired,
     updateStorageUnit: PropTypes.func.isRequired
   }

   constructor(props) {
     super(props)
     this.state = { storageUnit: this.props.storageUnit }
   }

   updateStorageUnit(data, key, value) {
     data[key] = value
     const b = { ...data }
     console.log(b)
     this.setState(b)
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
            updateType = {(storageType) => this.state.updateStorageUnit(this.state.storageUnit, 'storageType', storageType)}
            updateName= {(storageUnitName) =>
              this.updateStorageUnit(this.state.storageUnit, 'storageUnitName', storageUnitName)}
            updateAreal1= {(area) =>
              this.updateStorageUnit(this.state.storageUnit, 'area', parseFloat(area))}
            updateAreal2= {(areal2) =>
              this.updateStorageUnit(this.state.storageUnit, 'area2', parseFloat(areal2))}
            updateHeight1= {(height) =>
              this.updateStorageUnit(this.state.storageUnit, 'height', parseFloat(height))}
            updateHeight2= {(height2) =>
              this.updateStorageUnit(this.state.storageUnit, 'height2', parseFloat(height2))}
          />
          <Options
            unit = {{ skallsikring: this.state.storageUnit.skallsikring,
                     tyverisikring: this.state.storageUnit.tyverisikring,
                     brannsikring: this.state.storageUnit.brannsikring,
                     vannskaderisiko: this.state.storageUnit.vannskaderisiko,
                     rutinerBeredskap: this.state.storageUnit.rutinerBeredskap,
                     luftfuktighet: this.state.storageUnit.luftfuktighet,
                     lysforhold: this.state.storageUnit.lysforhold,
                     temperatur: this.state.storageUnit.temperatur,
                     preventivKonservering: this.state.storageUnit.preventivKonservering,
            }}
            updateSkallsikring={(skallsikring) =>
              this.state.updateStorageUnit(this.state.storageUnit, 'skallsikring', skallsikring)}
            updateTyverisikring={(tyverisikring) =>
              this.state.updateStorageUnit(this.state.storageUnit, 'tyverisikring', tyverisikring)}
            updateBrannsikring={(brannsikring) =>
              this.state.updateStorageUnit(this.state.storageUnit, 'brannsikring', brannsikring)}
            updateVannskaderisiko={(vannskaderisiko) =>
              this.state.updateStorageUnit(this.state.storageUnit, 'vannskaderisiko', vannskaderisiko)}
            updateRutinerBeredskap={(rutinerBeredskap) =>
              this.state.updateStorageUnit(this.state.storageUnit, 'rutinerBeredskap', rutinerBeredskap)}
            updateLuftfuktighet={(luftfuktighet) =>
              this.state.updateStorageUnit(this.state.storageUnit, 'luftfuktighet', luftfuktighet)}
            updateLysforhold={(lysforhold) =>
              this.state.updateStorageUnit(this.state.storageUnit, 'lysforhold', lysforhold)}
            updateTemperatur={(temperatur) =>
              this.state.updateStorageUnit(this.state.storageUnit, 'temperatur', temperatur)}
            updatePreventivKonservering={(preventivKonservering) =>
              this.state.updateStorageUnit(this.state.storageUnit, 'preventivKonservering', preventivKonservering)}
          />
        </main>
      </div>
      );
   }
}
