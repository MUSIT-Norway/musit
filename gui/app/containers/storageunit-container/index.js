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
 // import { insert as insertStorageUnitContainer } from '../../reducers/storageunit-container';
 import { load } from '../../reducers/storageunit-container';
 import { ButtonToolbar, Button } from 'react-bootstrap'

 const mapStateToProps = (state) => {
   return {
     storageUnit: state.storageUnit,
     sikringBevaring: state.sikringBevaring
   }
 }

 const mapDispatchToProps = (dispatch) => {

   return {
     onLagreClick: () => {
       // dispatch(insertStorageUnitContainer(this.state))
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
     sikringBevaring: PropTypes.object.isRequired,
     onLagreClick: PropTypes.func.isRequired
   };

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
       storageUnit: {
         type: '',
         name: 'Hei hei',
         areal1: '',
         areal2: '',
         height1: '',
         height2: ''
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
       }
     }
   }

   componentWillMount() {
     this.props.loadStorageUnit(1)
   }

   render() {
     return (
      <div>
        <main>
          <ButtonToolbar>
            <Button bsStyle="primary" onClick= {this.onLagreClick}>Lagre</Button>
            <Button>Cancel</Button>
          </ButtonToolbar>
          <StorageUnitComponents
            unit= { this.state.storageUnit }
            updateType= {(type) =>
              this.setState({ storageUnit: { ...this.state.storageUnit, type } })}
            updateName= {(name) =>
              this.setState({ storageUnit: { ...this.state.storageUnit, name } })}
            updateAreal1= {(areal1) =>
              this.setState({ storageUnit: { ...this.state.storageUnit, areal1 } })}
            updateAreal2= {(areal2) =>
              this.setState({ storageUnit: { ...this.state.storageUnit, areal2 } })}
            updateHeight1= {(height1) =>
              this.setState({ storageUnit: { ...this.state.storageUnit, height1 } })}
            updateHeight2= {(height2) =>
              this.setState({ storageUnit: { ...this.state.storageUnit, height2 } })}
          />
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
        </main>
      </div>
      );
   }
}
