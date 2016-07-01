import { connect } from 'react-redux';
import Language from '../../../components/language'
import { loadAll, deleteUnit } from '../../../reducers/storageunit'
import { add } from '../../../reducers/picklist'
import StorageUnitsContainerImpl from './StorageUnitsContainer'

const setName = (unit) => {
  unit.name = unit.storageUnitName
  return unit
};
const mapStateToProps = (state) => ({
  translate: (key, markdown) => Language.translate(key, markdown),
  units: state.storageUnit.data
})
const mapDispatchToProps = (dispatch) => ({
  loadStorageUnits: () => dispatch(loadAll()),
  onPick: (unit) => dispatch(add('default', setName(unit))),
  onEdit: (unit) => dispatch(add('default', unit)),
  onDelete: (unit) => dispatch(deleteUnit(unit.id))
})
@connect(mapStateToProps, mapDispatchToProps)
export default class StorageUnitsContainer extends StorageUnitsContainerImpl {}