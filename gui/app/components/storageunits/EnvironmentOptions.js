import React from 'react'
import { FormGroup, Checkbox, ControlLabel } from 'react-bootstrap'
export default class EnvironmentOptions extends React.Component {
  static propTypes = {
    unit: React.PropTypes.shape({
      skallsikring: React.PropTypes.bool,
      tyverisikring: React.PropTypes.bool,
      brannsikring: React.PropTypes.bool,
      vannskaderisiko: React.PropTypes.bool,
      rutinerBeredskap: React.PropTypes.bool,
      luftfuktighet: React.PropTypes.bool,
      lysforhold: React.PropTypes.bool,
      temperatur: React.PropTypes.bool,
      preventivKonservering: React.PropTypes.bool,
    }),
    updateSkallsikring: React.PropTypes.func.isRequired,
    updateTyverisikring: React.PropTypes.func.isRequired,
    updateBrannsikring: React.PropTypes.func.isRequired,
    updateVannskaderisiko: React.PropTypes.func.isRequired,
    updateRutinerBeredskap: React.PropTypes.func.isRequired,
    updateLuftfuktighet: React.PropTypes.func.isRequired,
    updateLysforhold: React.PropTypes.func.isRequired,
    updateTemperatur: React.PropTypes.func.isRequired,
    updatePreventivKonservering: React.PropTypes.func.isRequired,
  }

  render() {
    return (
      <div>
        <FormGroup>
          <ControlLabel>Sikring</ControlLabel>

          <Checkbox
            checked={this.props.unit.skallsikring}
            onClick={(event) => this.props.updateSkallsikring(event.target.checked)}
          >
            Skallsikring
          </Checkbox>
          <Checkbox
            checked={this.props.unit.tyverisikring}
            onClick={(event) => this.props.updateTyverisikring(event.target.checked)}
          >
            Tyverisikring
          </Checkbox>
          <Checkbox
            checked={this.props.unit.brannsikring}
            onClick={(event) => this.props.updateBrannsikring(event.target.checked)}
          >
            Brannsikring
          </Checkbox>
          <Checkbox
            checked={this.props.unit.vannskaderisiko}
            onClick={(event) => this.props.updateVannskaderisiko(event.target.checked)}
          >
            Vannskaderisiko
          </Checkbox>
          <Checkbox
            checked={this.props.unit.rutinerBeredskap}
            onClick={(event) => this.props.updateRutinerBeredskap(event.target.checked)}
          >
            Rutiner/beredskap
          </Checkbox>

          <ControlLabel>Bevaring</ControlLabel>

          <Checkbox
            checked={this.props.unit.luftfuktighet}
            onClick={(event) => this.props.updateLuftfuktighet(event.target.checked)}
          >
            Luftfuktighet
          </Checkbox>
          <Checkbox
            checked={this.props.unit.lysforhold}
            onClick={(event) => this.props.updateLysforhold(event.target.checked)}
          >
            Lysforhold
          </Checkbox>
          <Checkbox
            checked={this.props.unit.temperatur}
            onClick={(event) => this.props.updateTemperatur(event.target.checked)}
          >
            Temperatur
          </Checkbox>
          <Checkbox
            checked={this.props.unit.preventivKonservering}
            onClick={(event) => this.props.updatePreventivKonservering(event.target.checked)}
          >
            Preventiv Konservering
          </Checkbox>
        </FormGroup>


      </div>
    )
  }
          }
