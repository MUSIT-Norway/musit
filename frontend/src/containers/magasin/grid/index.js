import React from 'react'
import { connect } from 'react-redux';
import Language from '../../../components/language'
import { loadAll, deleteUnit } from '../../../reducers/storageunit/grid'
import { add } from '../../../reducers/picklist'
import { browserHistory } from 'react-router'
import { NodeGrid } from '../../../components/grid'
import Layout from '../../../layout'
import NodeLeftMenuComponent from '../../../components/leftmenu/node'
import Toolbar from './Toolbar'

const mapStateToProps = (state) => ({
  translate: (key, markdown) => Language.translate(key, markdown),
  units: state.storageGridUnit.data || []
})
const mapDispatchToProps = (dispatch) => ({
  loadStorageUnits: () => dispatch(loadAll()),
  onPick: (unit) => dispatch(add('default', unit)),
  onEdit: (unit) => { browserHistory.push(`/storageunit/${unit.id}`) },
  onDelete: (unit) => dispatch(deleteUnit(unit.id))
})
@connect(mapStateToProps, mapDispatchToProps)
export default class StorageUnitsContainer extends React.Component {
  static propTypes = {
    units: React.PropTypes.arrayOf(React.PropTypes.object),
    translate: React.PropTypes.func.isRequired,
    loadStorageUnits: React.PropTypes.func.isRequired,
    onDelete: React.PropTypes.func.isRequired,
    onEdit: React.PropTypes.func.isRequired,
    onPick: React.PropTypes.func.isRequired
  }

  componentWillMount() {
    this.props.loadStorageUnits()
  }

  render() {
    return (
      <Layout
        title={"Magasin"}
        translate={this.props.translate}
        breadcrumb={"Museum / Papirdunken / Esken inn i der"}
        toolbar={<Toolbar />}
        leftMenu={<NodeLeftMenuComponent
          id="1"
          translate={this.props.translate}
          onClickNewNode={(key) => key}
          objectsOnNode={11}
          totalObjectCount={78}
          underNodeCount={5}
          onClickProperties={(key) => key}
          onClickObservations={(key) => key}
          onClickController={(key) => key}
          onClickMoveNode={(key) => key}
          onClickDelete={this.props.onDelete}
        />}
        content={<NodeGrid
          id="1"
          translate={this.props.translate}
          tableData={this.props.units}
        />}
      />
    )
  }
}
