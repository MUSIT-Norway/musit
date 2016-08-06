import React from 'react'
import { connect } from 'react-redux';
import Language from '../../../components/language'
import { loadAll, loadChildren, deleteUnit } from '../../../reducers/storageunit/grid'
import { add } from '../../../reducers/picklist'
import { hashHistory } from 'react-router'
import { NodeGrid, ObjectGrid } from '../../../components/grid'
import Layout from '../../../layout'
import NodeLeftMenuComponent from '../../../components/leftmenu/node'
import Toolbar from './Toolbar'

const mapStateToProps = (state) => ({
  translate: (key, markdown) => Language.translate(key, markdown),
  units: state.storageGridUnit.data || []
})

const mapDispatchToProps = (dispatch) => ({
  loadStorageUnits: () => dispatch(loadAll()),
  loadChildren: (id) => dispatch(loadChildren(id)),
  onPick: (unit) => dispatch(add('default', unit)),
  onItemClick: (unit) => { hashHistory.push(`/magasin/${unit.id}`) },
  onEdit: (unit) => { hashHistory.push(`/storageunit/${unit.id}`) },
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
    onPick: React.PropTypes.func.isRequired,
    props: React.PropTypes.object,
    params: React.PropTypes.object,
    history: React.PropTypes.object,
    loadChildren: React.PropTypes.func
  }

  constructor(props) {
    super(props)
    this.state = {
      showObjects: false,
      showNodes: true
    }
  }

  componentWillMount() {
    if (this.props.params.id) {
      this.props.loadChildren(this.props.params.id)
    } else {
      this.props.loadStorageUnits()
    }
  }

  render() {
    return (
      <Layout
        title={"Magasin"}
        translate={this.props.translate}
        breadcrumb={"Museum / Papirdunken / Esken inni der"}
        toolbar={<Toolbar
          showObjects={this.state.showObjects}
          showNodes={this.state.showNodes}
          clickShowObjects={() => this.setState({ ...this.state, showObjects: true, showNodes: false })}
          clickShowNodes={() => this.setState({ ...this.state, showObjects: false, showNodes: true })}
        />}
        leftMenu={<NodeLeftMenuComponent
          id="1"
          translate={this.props.translate}
          onClickNewNode={(key) => key}
          objectsOnNode={11}
          totalObjectCount={78}
          underNodeCount={5}
          onClickProperties={(key) => key}
          onClickObservations={(id) => this.props.history.push(`/grid/observationcontrol/${id}`)}
          onClickController={(id) => this.props.history.push(`/grid/observationcontrol/${id}`)}
          onClickMoveNode={(key) => key}
          onClickDelete={this.props.onDelete}
        />}
        content={this.state.showNodes ? <NodeGrid
          id="1"
          translate={this.props.translate}
          tableData={this.props.units}
          onPick={this.props.onPick}
          onDelete={this.props.onDelete}
          onItemClick={(unit) => {
            this.props.loadChildren(unit.id)
            this.props.history.push(`/magasin/${unit.id}`)
          }}
        /> : <ObjectGrid
          translate={this.props.translate}
          tableData={[]}
        />}
      />
    )
  }
}
