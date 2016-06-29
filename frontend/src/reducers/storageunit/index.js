const INSERT = 'musit/storageunit-container/INSERT';
const UPDATE = 'musit/storageunit-container/UPDATE';
const INSERT_SUCCESS = 'musit/storageunit-container/INSERT_SUCCESS';
const INSERT_FAIL = 'musit/storageunit-container/INSERT_FAIL';
const LOAD = 'musit/storageunit-container/LOAD';
const LOAD_SUCCESS = 'musit/storageunit-container/LOAD_SUCCESS';
const LOAD_FAIL = 'musit/storageunit-container/LOAD_FAIL';
const LOADALL = 'musit/storageunit-container/LOADALL'
const LOADALL_SUCCESS = 'musit/storageunit-container/LOADALL_SUCCESS'
const LOADALL_FAIL = 'musit/storageunit-container/LOADALL_FAIL'
const DELETE = 'musit/storageunit-container/DELETE'
const DELETE_SUCCESS = 'musit/storageunit-container/DELETE_SUCCESS'
const DELETE_FAIL = 'musit/storageunit-container/DELETE_FAIL'

const initialState = {}

const storageInsertUnitContainerReducer = (state = initialState, action = {}) => {
  switch (action.type) {
    case INSERT:
      return {
        ...state,
        loading: true,
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
        data: action.error
      };
    case UPDATE:
      return {
        ...state,
        loading: false,
        loaded: true,
        data: action.data
      };
    case LOAD:
      return {
        ...state,
        loading: true,
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
      }
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

export default storageInsertUnitContainerReducer;

export const isLoaded = (globalState) => {
  return globalState.storageUnitContainer && globalState.storageUnitContainer.loaded;
}

export const load = (id) => {
  return {
    types: [LOAD, LOAD_SUCCESS, LOAD_FAIL],
    promise: (client) => client.get(`/api/storageadmin/v1/storageunit/${id}`)
  };
}

export const loadAll = () => {
  return {
    types: [LOADALL, LOADALL_SUCCESS, LOADALL_FAIL],
    promise: (client) => client.get('/api/storageadmin/v1/storageunit')
  };
}

export const insert = (data) => {
  return {
    types: [INSERT, INSERT_SUCCESS, INSERT_FAIL],
    promise: (client) => client.post('/api/storageadmin/v1/storageunit', { data })
  };
}

export const deleteUnit = (id) => {
  return {
    types: [DELETE, DELETE_SUCCESS, DELETE_FAIL],
    promise: (client) => client.del(`/api/storageadmin/v1/storageunit/${id}`),
    id
  };
}

export const update = (data) => {
  return {
    type: UPDATE,
    data: data
  }
}
