import React from 'react'
import { connect } from 'react-redux';
import Language from '../../../components/language'
import { loadRoot, clearRoot, loadChildren, deleteUnit } from '../../../reducers/storageunit/grid'
import { add } from '../../../reducers/picklist'
import { hashHistory } from 'react-router'
import { NodeGrid, ObjectGrid } from '../../../components/grid'
import Layout from '../../../layout'
import NodeLeftMenuComponent from '../../../components/leftmenu/node'
import Toolbar from '../../../layout/Toolbar'

const mapStateToProps = (state) => ({
  translate: (key, markdown) => Language.translate(key, markdown),
  children: state.storageGridUnit.data || [],
  rootNode: state.storageGridUnit.root,
  unit: {
    id: 1,
    name: 'Root'
  }
})

const mapDispatchToProps = (dispatch) => ({
  loadStorageUnits: () => {
    dispatch(clearRoot())
    dispatch(loadRoot())
  },
  loadChildren: (id) => {
    dispatch(loadChildren(id))
    dispatch(loadRoot(id))
  },
  onPick: (unit) => dispatch(add('default', unit)),
  onItemClick: (unit) => { hashHistory.push(`/magasin/${unit.id}`) },
  onEdit: (unit) => { hashHistory.push(`/storageunit/${unit.id}`) },
  onDelete: (unit) => dispatch(deleteUnit(unit.id))
})

@connect(mapStateToProps, mapDispatchToProps)
export default class StorageUnitsContainer extends React.Component {
  static propTypes = {
    children: React.PropTypes.arrayOf(React.PropTypes.object),
    unit: React.PropTypes.shape({
      id: React.PropTypes.number.isRequired,
      name: React.PropTypes.string.isRequired
    }),
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
      searchPattern: '',
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

  makeToolbar() {
    return (<Toolbar
      showRight={this.state.showObjects}
      showLeft={this.state.showNodes}
      labelRight="Objekter"
      labelLeft="Noder"
      placeHolderSearch="Filtrer i liste"
      searchValue={this.state.searchPattern}
      onSearchChanged={(newPattern) => this.setState({ ...this.state, searchPattern: newPattern })}
      clickShowRight={() => this.setState({ ...this.state, showObjects: true, showNodes: false })}
      clickShowLeft={() => this.setState({ ...this.state, showObjects: false, showNodes: true })}
    />)
  }

  makeLeftMenu() {
    const { rootNode } = this.state

    return (<div style={{ paddingTop: 10 }}>
      <NodeLeftMenuComponent
        id={rootNode ? rootNode.data.id : null}
        translate={this.props.translate}
        onClickNewNode={() => this.props.history.push('/storageunit/add')}
        objectsOnNode={rootNode ? rootNode.statistics.objectsOnNode : null}
        totalObjectCount={rootNode ? rootNode.statistics.totalObjectCount : null}
        underNodeCount={rootNode ? rootNode.statistics.underNodeCount : null}
        onClickProperties={(key) => key}
        onClickObservations={(id) => this.props.history.push(`/observationcontrol/${id}`)}
        onClickController={(id) => this.props.history.push(`/observationcontrol/${id}`)}
        onClickMoveNode={(key) => key}
        onClickDelete={this.props.onDelete}
      />
    </div>)
  }

  makeContentGrid(filter) {
    if (this.state.showNodes) {
      return (<NodeGrid
        id={this.props.unit.id}
        translate={this.props.translate}
        tableData={this.props.children.filter((row) => row.name.indexOf(filter) !== -1)}
        onPick={this.props.onPick}
        onClick={(unit) =>
          this.props.history.push(`/magasin/${unit.id}`)
        }
      />)
    }
    return (<ObjectGrid
      id={this.props.unit.id}
      translate={this.props.translate}
      tableData={[]}
    />)
  }

  render() {
    const { searchPattern } = this.state
    return (
      <Layout
        title={"Magasin"}
        translate={this.props.translate}
        breadcrumb={"Museum / Papirdunken / Esken inni der"}
        toolbar={this.makeToolbar()}
        leftMenu={this.makeLeftMenu()}
        content={this.makeContentGrid(searchPattern)}
      />
    )
  }
}
