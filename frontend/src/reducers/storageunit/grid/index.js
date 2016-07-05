const LOADALL = 'musit/storageunit-grid/LOADALL'
const LOADALL_SUCCESS = 'musit/storageunit-grid/LOADALL_SUCCESS'
const LOADALL_FAIL = 'musit/storageunit-grid/LOADALL_FAIL'
const DELETE = 'musit/storageunit-grid/DELETE'
const DELETE_SUCCESS = 'musit/storageunit-grid/DELETE_SUCCESS'
const DELETE_FAIL = 'musit/storageunit-grid/DELETE_FAIL'

const initialState = {}

const storageUnitGridReducer = (state = initialState, action = {}) => {
  switch (action.type) {
    case LOADALL:
      return {
        ...state,
        loading: true
      }
    case LOADALL_SUCCESS:
      return {
        ...state,
        loading: false,
        loaded: true,
        data: action.result
      }
    case LOADALL_FAIL:
      return {
        ...state,
        loading: false,
        loaded: false,
        error: action.error
      }
    case DELETE:
      return {
        ...state,
        loading: true
      }
    case DELETE_SUCCESS:
      return {
        ...state,
        loading: false,
        loaded: true,
        data: state.data.filter(d => d.id !== action.id)
      }
    case DELETE_FAIL:
      return {
        ...state,
        loading: false,
        loaded: false,
        error: action.error
      }
    default:
      return state
  }
}

export default storageUnitGridReducer;

export const isLoaded = (globalState) => {
  return globalState.storageUnitContainer && globalState.storageUnitContainer.loaded;
}

export const loadAll = () => {
  return {
    types: [LOADALL, LOADALL_SUCCESS, LOADALL_FAIL],
    promise: (client) => client.get('/api/storageadmin/v1/storageunit')
  };
}

export const deleteUnit = (id) => {
  return {
    types: [DELETE, DELETE_SUCCESS, DELETE_FAIL],
    promise: (client) => client.del(`/api/storageadmin/v1/storageunit/${id}`),
    id
  };
}
