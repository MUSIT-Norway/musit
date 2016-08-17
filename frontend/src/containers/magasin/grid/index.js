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
import Breadcrumb from 'react-breadcrumbs'

const mapStateToProps = (state) => ({
  translate: (key, markdown) => Language.translate(key, markdown),
  children: state.storageGridUnit.data || [],
  rootNode: state.storageGridUnit.root,
  routerState: state.router
})

const mapDispatchToProps = (dispatch, props) => {
  const { history } = props

  return ({
    loadStorageUnits: () => {
      dispatch(clearRoot())
      dispatch(loadRoot())
    },
    loadChildren: (id) => {
      dispatch(loadChildren(id))
      dispatch(loadRoot(id))
    },
    onAction: (actionName, unit) => {
      switch (actionName) {
        case 'pick':
          dispatch(add('default', unit))
          break
        case 'observation':
          history.push(`/observationcontrol/${unit.id}`)
          break
        case 'control':
          history.push(`/observationcontrol/${unit.id}`)
          break
        case 'move':
          /* TODO: Add move route or action */
          break
        default:
          break
      }
    },
    onEdit: (unit) => { hashHistory.push(`/storageunit/${unit.id}`) },
    onDelete: (id, currentNode) => { // TODO: Problems with delete slower then callback (async)
      if (id === currentNode.id) {
        dispatch(deleteUnit(id, {
          onSuccess: () => {
            dispatch(clearRoot())
            if (currentNode.isPartOf) {
              dispatch(loadChildren(currentNode.isPartOf))
              dispatch(loadRoot(currentNode.isPartOf))
            } else {
              dispatch(loadRoot())
            }
          }
        }))
      }
    }
  })
}

@connect(mapStateToProps, mapDispatchToProps)
export default class StorageUnitsContainer extends React.Component {
  static propTypes = {
    children: React.PropTypes.arrayOf(React.PropTypes.object),
    rootNode: React.PropTypes.object,
    translate: React.PropTypes.func.isRequired,
    loadStorageUnits: React.PropTypes.func.isRequired,
    onDelete: React.PropTypes.func.isRequired,
    onEdit: React.PropTypes.func.isRequired,
    onAction: React.PropTypes.func.isRequired,
    props: React.PropTypes.object,
    params: React.PropTypes.object,
    history: React.PropTypes.object,
    routerState: React.PropTypes.object,
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
    // Issued on initial render of the component
    if (this.props.params.splat) {
      this.props.loadChildren(this.resolveCurrentId(this.props.params.splat))
    } else {
      this.props.loadStorageUnits()
    }
  }

  componentWillReceiveProps(newProps) {
    // Issued on every propchange, including local route changes
    if (newProps.params.splat !== this.props.params.splat) {
      if (newProps.params.splat) {
        this.props.loadChildren(this.resolveCurrentId(newProps.params.splat))
      } else {
        this.props.loadStorageUnits()
      }
    }
  }

  resolveCurrentId(splat) {
    const ids = this.resolveId(splat)
    let retVal = null
    if (ids && ids.length > 0) {
      retVal = ids[ids.length - 1]
    }
    return retVal
  }

  resolveId(splat) {
    let splatList = []
    if (splat) {
      splatList = splat.split('/')
    }
    return splatList
  }

  pathChild(splat, id) {
    let newUri = `${id}`
    if (splat) {
      newUri = `${splat}/${id}`
    }
    return newUri
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

  makeLeftMenu(rootNode, statistics) {
    const { onEdit, onDelete, history } = this.props

    return (<div style={{ paddingTop: 10 }}>
      <NodeLeftMenuComponent
        id={rootNode ? rootNode.id : null}
        translate={this.props.translate}
        onClickNewNode={(parentId) => {
          if (parentId) {
            history.push(`/storageunit/${parentId}/add`)
          } else {
            history.push('/storageunit/add')
          }
        }}
        objectsOnNode={statistics ? statistics.objectsOnNode : Number.NaN}
        totalObjectCount={statistics ? statistics.totalObjectCount : Number.NaN}
        underNodeCount={statistics ? statistics.underNodeCount : Number.NaN}
        onClickProperties={(id) => onEdit({ id })}
        onClickObservations={(id) => history.push(`/observationcontrol/${id}`)}
        onClickController={(id) => history.push(`/observationcontrol/${id}`)}
        onClickMoveNode={(id) => id/* TODO: Add move action for rootnode*/}
        onClickDelete={(id) => onDelete(id, rootNode)}
      />
    </div>)
  }

  makeContentGrid(filter, rootNode, children) {
    if (this.state.showNodes) {
      return (<NodeGrid
        id={rootNode ? rootNode.id : 0}
        translate={this.props.translate}
        tableData={children.filter((row) => row.name.indexOf(filter) !== -1)}
        onAction={this.props.onAction}
        onClick={(row) =>
          this.props.history.replace(
            `/magasin/${this.pathChild(this.props.params.splat, row.id)}`
          )}
      />)
    }
    return (<ObjectGrid
      id={rootNode ? rootNode.id : 0}
      translate={this.props.translate}
      tableData={[]}
    />)
  }

  makeBreadcrumb(router) {
    return (<Breadcrumb
      routes={router.routes}
      params={router.params}
    />)
  }

  render() {
    // breadcrumb={this.makeBreadcrumb(routerState)}
    const { searchPattern } = this.state
    const { children, translate } = this.props
    const { data: rootNodeData, statistics } = this.props.rootNode
    return (
      <Layout
        title={"Magasin"}
        translate={translate}
        toolbar={this.makeToolbar()}
        leftMenu={this.makeLeftMenu(rootNodeData, statistics)}
        content={this.makeContentGrid(searchPattern, rootNodeData, children)}
      />
    )
  }
}
