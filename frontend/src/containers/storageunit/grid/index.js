import { connect } from 'react-redux';
import Language from '../../../components/language'
import { loadAll, deleteUnit } from '../../../reducers/storageunit/grid'
import { add } from '../../../reducers/picklist'
import StorageUnitsContainerImpl from './StorageUnitsContainer'
import { browserHistory } from 'react-router'

const mapStateToProps = (state) => ({
  translate: (key, markdown) => Language.translate(key, markdown),
  units: state.storageGridUnit.data
})
const mapDispatchToProps = (dispatch) => ({
  loadStorageUnits: () => dispatch(loadAll()),
  onPick: (unit) => dispatch(add('default', unit)),
  onEdit: (unit) => { browserHistory.push(`/storageunit/${unit.id}`) },
  onDelete: (unit) => dispatch(deleteUnit(unit.id))
})
@connect(mapStateToProps, mapDispatchToProps)
export default class StorageUnitsContainer extends StorageUnitsContainerImpl {}
