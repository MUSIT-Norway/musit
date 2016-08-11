import { mapToBackend } from './mapper/to_backend'

const ADD = 'musit/control/ADD'
const ADD_SUCCESS = 'musit/contol/ADD_SUCCESS'
const ADD_FAILURE = 'musit/contol/ADD_FAILURE'

const initialState = {
  user: '',
  date: '',
  data: {
    temperatureOK: null,
    inertAirOK: null,
    gasOK: null,
    cleaningOK: null,
    relativeHumidity: null,
    lightConditionsOK: null,
    alchoholOK: null,
    pestOK: null,
    moldFungusOK: null
  }
}

const controlReducer = (state = initialState, action = {}) => {
  switch (action.type) {
    case ADD:
      const data = mapToBackend(action.data)
      return {
        ...state,
        loading: true,
        loaded: false,
        data
      };
    default:
      return state;
  }
}

export default controlReducer;

export const addControl = (data) => {
  return {
    types: [ADD, ADD_SUCCESS, ADD_FAILURE],
    promise: (client) => client.post('/api/event/v1/event', { data })
  }
}
