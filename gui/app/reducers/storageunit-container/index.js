const INSERT = 'musit/storageunit-container/LOAD';
const INSERT_SUCCESS = 'musit/storageunit-container/LOAD_SUCCESS';
const INSERT_FAIL = 'musit/storageunit-container/LOAD_FAIL';


const initialState = []

const storageInsertUnitContainerReducer = (state = initialState, action = {}) => {
  switch (action.type) {
    case INSERT:
      return {
        ...state,
        loading: true
      };
    case INSERT_SUCCESS:
      return {
        ...state,
        loading: false,
        loaded: true,
        data: action.result
      };
    case INSERT_FAIL:
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

export default storageInsertUnitContainerReducer;

export const isLoaded = (globalState) => {
  return globalState.storageUnitContainer && globalState.storageUnitContainer.loaded;
}

export const insert = (state) => {
  console.log(state)
  return {
    types: [INSERT, INSERT_SUCCESS, INSERT_FAIL],
    promise: (client) => client.get('/storageInsertUnitContainerReducer')
  };
}
