import React, { Component } from 'react'
import ObservationControlComponent from '../../../components/leftmenu/observationcontrol'
import { connect } from 'react-redux'
import Language from '../../../components/language'

const mapStateToProps = () => ({
  translate: (key, markdown) => Language.translate(key, markdown)
})

@connect(mapStateToProps)

export default class ObservationControl extends Component {
  static propTypes = {
    translate: React.PropTypes.func.isRequired
  }

  constructor(props) {
    super(props);
    this.state = {
      selectObservation: {
        checked: false
      },
      selectControl: {
        checked: false
      }
    };
  }

  render() {
    const { translate } = this.props
    return (
      <div>
        <ObservationControlComponent
          id="1"
          translate={translate}
          selectObservation={this.state.selectObservation.checked}
          selectControl={this.state.selectControl.checked}
          onClickNewObservation={(key) => key}
          onClickNewControl={(key) => key}
          onClickSelectObservation={
            () => this.setState({ selectObservation: { checked: !this.state.selectObservation.checked } })
          }
          onClickSelectControl={
            () => this.setState({ selectControl: { checked: !this.state.selectControl.checked } })
          }
        />
      </div>
    )
  }
}
