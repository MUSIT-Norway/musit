import React, { Component } from 'react'
import NodeLeftMenuComponent from '../../../components/leftmenu/node'
import { connect } from 'react-redux'
import Language from '../../../components/language'

const mapStateToProps = () => ({
  translate: (key, markdown) => Language.translate(key, markdown)
})

@connect(mapStateToProps)

export default class NodeLeftMenu extends Component {
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
        <NodeLeftMenuComponent
          id="1"
          translate={translate}
          onClickNewNode={(key) => key}
          objectsOnNode="11"
          totalObjectCount="78"
          underNodeCount="5"
          onClickProperties={(key) => key}
          onClickObservations={(key) => key}
          onClickController={(key) => key}
          onClickMoveNode={(key) => key}
          onClickDelete={(key) => key}
        />
      </div>
    )
  }
}
