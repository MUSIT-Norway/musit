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
import Options from '../../../components/storageunits/EnvironmentOptions'
import StorageUnitComponents from '../../../components/storageunits/StorageUnitComponent'
import { ButtonToolbar, Button, Grid, Row } from 'react-bootstrap'
import EnvironmentRequirementComponent from '../../../components/storageunits/EnvironmentRequirementComponent'

export default class StorageUnitContainer extends Component {
  static propTypes = {
    unit: PropTypes.object.isRequired,
    id: PropTypes.number,
    loadStorageUnit: PropTypes.func.isRequired,
    onAddressSuggestionsUpdateRequested: PropTypes.func.isRequired,
    suggest: React.PropTypes.array.isRequired,
    params: PropTypes.object,
    onLagreClick: PropTypes.func.isRequired,
    translate: PropTypes.func.isRequired,
  }


  componentWillMount() {
    this.props.loadStorageUnit(this.props.params.id)
  }

  updateStorageUnit(data, key, value) {
    const newData = Object.assign({}, data);
    newData[key] = value
    this.setState({ unit: newData })
  }

  render() {
    const data = (this.state && this.state.unit) ? this.state.unit : this.props.unit;
    return (
      <div>
        <main>
          <Grid>
            <Row styleClass="row-centered">
              <ButtonToolbar>
                <Button bsStyle="primary" onClick={() => this.props.onLagreClick(data)} >Lagre</Button>
                <Button onClick={() => window.history.back()}>Cancel</Button>
              </ButtonToolbar>
            </Row>
          </Grid>
          <StorageUnitComponents
            unit={data}
            translate={this.props.translate}
            updateType={(type) =>
              this.updateStorageUnit(data, 'type', type)}
            updateName={(name) =>
              this.updateStorageUnit(data, 'name', name)}
            updateAreal1={(area) =>
              this.updateStorageUnit(data, 'area', area !== '' && /^-?(\d+\.?\d*)$|(\d*\.?\d+)$/.test(area) ? Number(area.replace(',', '.')) : area)}
            updateAreal2={(areaTo) =>
              this.updateStorageUnit(data, 'areaTo', areaTo !== '' && /^-?(\d+\.?\d*)$|(\d*\.?\d+)$/.test(areaTo) ? Number(areaTo.replace(',', '.')) : areaTo)}
            updateHeight1={(height) =>
              this.updateStorageUnit(data, 'height', height !== '' && /^-?(\d+\.?\d*)$|(\d*\.?\d+)$/.test(height) ? Number(height.replace(',', '.')) : height)}
            updateHeight2={(heightTo) =>
              this.updateStorageUnit(data, 'heightTo', heightTo !== '' && /^-?(\d+\.?\d*)$|(\d*\.?\d+)$/.test(heightTo) ? Number(heightTo.replace(',', '.')) : heightTo)}
            updateAddress={(address) =>
              this.updateStorageUnit(data, 'address', address)}
            onAddressSuggestionsUpdateRequested={this.props.onAddressSuggestionsUpdateRequested}
            suggest={this.props.suggest}
          />
          <EnvironmentRequirementComponent
            translate={this.props.translate}
          />
          {data.type === 'Room' ?
            <Options
              unit={data}
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
            : null}
        </main>
      </div>
    );
  }
}
