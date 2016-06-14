import React from 'react'
import { Checkbox, ControlLabel, Grid, Row, Col } from 'react-bootstrap'
export default class EnvironmentOptions extends React.Component {
  static propTypes = {
    unit: {
      sikringSkallsikring: React.PropTypes.bool,
      tyverisikring: React.PropTypes.bool,
      brannsikring: React.PropTypes.bool,
      vannskaderisiko: React.PropTypes.bool,
      rutinerBeredskap: React.PropTypes.bool,
      luftfuktighet: React.PropTypes.bool,
      lysforhold: React.PropTypes.bool,
      temperatur: React.PropTypes.bool,
      preventivKonservering: React.PropTypes.bool,
    },
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
      <Grid>
        <Row styleClass="row-centered">
          <Col lg={6} md={6} sm={6} xs={12}>
            <ControlLabel>Sikring</ControlLabel>

            <Checkbox
              checked={this.props.unit.sikringSkallsikring}
              onChange={(event) => this.props.updateSkallsikring(event.target.checked)}
            >
              Skallsikring
            </Checkbox>
            <Checkbox
              checked={this.props.unit.tyverisikring}
              onChange={(event) => this.props.updateTyverisikring(event.target.checked)}
            >
              Tyverisikring
            </Checkbox>
            <Checkbox
              checked={this.props.unit.brannsikring}
              onChange={(event) => this.props.updateBrannsikring(event.target.checked)}
            >
              Brannsikring
            </Checkbox>
            <Checkbox
              checked={this.props.unit.vannskaderisiko}
              onChange={(event) => this.props.updateVannskaderisiko(event.target.checked)}
            >
              Vannskaderisiko
            </Checkbox>
            <Checkbox
              checked={this.props.unit.rutinerBeredskap}
              onChange={(event) => this.props.updateRutinerBeredskap(event.target.checked)}
            >
              Rutiner/beredskap
            </Checkbox>
          </Col>
          <Col lg={6} md={6} sm={6} xs={12}>
            <ControlLabel>Bevaring</ControlLabel>

            <Checkbox
              checked={this.props.unit.luftfuktighet}
              onChange={(event) => this.props.updateLuftfuktighet(event.target.checked)}
            >
              Luftfuktighet
            </Checkbox>
            <Checkbox
              checked={this.props.unit.lysforhold}
              onChange={(event) => this.props.updateLysforhold(event.target.checked)}
            >
              Lysforhold
            </Checkbox>
            <Checkbox
              checked={this.props.unit.temperatur}
              onChange={(event) => this.props.updateTemperatur(event.target.checked)}
            >
              Temperatur
            </Checkbox>
            <Checkbox
              checked={this.props.unit.preventivKonservering}
              onChange={(event) => this.props.updatePreventivKonservering(event.target.checked)}
            >
              Preventiv Konservering
            </Checkbox>
          </Col>
        </Row>
      </Grid>
    )
  }
          }
