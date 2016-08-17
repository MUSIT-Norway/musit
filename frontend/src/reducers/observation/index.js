import { mapToFrontEnd, mapToBackEnd } from './mapper'
const ADD = 'musit/observation/ADD'
const ADD_SUCCESS = 'musit/observation/ADD_SUCCESS'
const ADD_FAIL = 'musit/observation/ADD_FAIL'
const LOAD = 'musit/observation/LOAD'
const LOAD_SUCCESS = 'musit/observation/LOAD_SUCCESS'
const LOAD_FAIL = 'musit/observation/LOAD_FAIL'

export const initialState = {
  data: {
    user: '',
    date: '',
    observations: []
  }
}

const observationReducer = (state = initialState, action = {}) => {
  let d = {}
  switch (action.type) {
    case ADD:
      return {
        ...state,
        loading: true,
        loaded: false,
        data: {}
      };
    case ADD_SUCCESS:
      d = mapToFrontEnd(action.result)
      return {
        ...state,
        loading: false,
        loaded: true,
        data: d
      };
    case ADD_FAIL:
      return {
        ...state,
        loading: false,
        loaded: false,
        data: action.error
      }
    case LOAD:
      return {
        ...state,
        loading: true,
        loaded: false,
        data: {}
      };
    case LOAD_SUCCESS:
      d = mapToFrontEnd(action.result)
      return {
        ...state,
        loading: false,
        loaded: true,
        data: d
      };
    case LOAD_FAIL:
      return {
        ...state,
        loading: false,
        loaded: false,
        data: action.error
      };
    default:
      return state;
  }
}

export default observationReducer;

export const addObservation = (data) => {
  const action = 'post'
  const url = '/api/event/v1/event'
  const dataToPost = mapToBackEnd(data)
  return {
    types: [ADD, ADD_SUCCESS, ADD_FAIL],
    promise: (client) => client[action](url, { data: dataToPost })
  };
}


export const loadObservation = (id) => {
  return {
    types: [LOAD, LOAD_SUCCESS, LOAD_FAIL],
    promise: (client) => client.get(`api/event/v1/event/${id}`)
  }
}
