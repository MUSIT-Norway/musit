import { connect } from 'react-redux';
import Language from '../../../components/language'
import { load, insert as insertStorageUnitContainer } from '../../../reducers/storageunit';
import { suggestAddress, clearSuggest } from '../../../reducers/suggest'
import StorageUnitContainerImpl from './StorageUnitContainer'

const mapStateToProps = (state) => {
  return {
    storageUnit: (state.storageUnit && state.storageUnit.data) ? state.storageUnit.data : {},
    translate: (key, markdown) => Language.translate(key, markdown),
    suggest: state.suggest
  }
}

const mapDispatchToProps = (dispatch) => {
  return {
    onLagreClick: (data) => {
      dispatch(insertStorageUnitContainer(data))
    },
    loadStorageUnit: (id) => {
      dispatch(load(id))
    },
    onAddressSuggestionsUpdateRequested: ({ value, reason }) => {
      // Should only autosuggest on typing if you have more then 3 characters
      if (reason && (reason === 'type') && value && value.length >= 3) {
        dispatch(suggestAddress('addressField', value))
      } else {
        dispatch(clearSuggest('addressField'))
      }
    }
  }
}

@connect(mapStateToProps, mapDispatchToProps)
export class StorageUnitContainer extends StorageUnitContainerImpl {}
