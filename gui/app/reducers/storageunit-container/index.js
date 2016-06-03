const LOAD = 'musit/storageunit-container/LOAD';
const LOAD_SUCCESS = 'musit/storageunit-container/LOAD_SUCCESS';
const LOAD_FAIL = 'musit/storageunit-container/LOAD_FAIL';


const initialState = {
  loaded: false
};

const storageUnitContainerReducer = (state = initialState, action = {}) => {
  switch (action.type) {
    case LOAD:
      return {
        ...state,
        loading: true
      };
    case LOAD_SUCCESS:
      return {
        ...state,
        loading: false,
        loaded: true,
        data: action.result
      };
    case LOAD_FAIL:
      return {
        ...state,
        loading: false,
        loaded: false,
        error: action.error
      };
    default:
      return state;
  }
}

export default storageUnitContainerReducer;

export const isLoaded = (globalState) => {
  return globalState.storageUnitContainer && globalState.storageUnitContainer.loaded;
}

export const load = () => {
  return {
    types: [LOAD, LOAD_SUCCESS, LOAD_FAIL],
    promise: (client) => client.get('/loadStorageUnitContainer')
  };
}
