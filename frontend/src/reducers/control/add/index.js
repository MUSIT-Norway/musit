import { mapToBackend } from './mapper/to_backend'

const ADD = 'musit/control/ADD'
const ADD_SUCCESS = 'musit/control/ADD_SUCCESS'
const ADD_FAIL = 'musit/control/ADD_FAILURE'

const initialState = {
  user: '',
  date: '',
  data: {
    temperatureOK: null,
    inertAirOK: null,
    gasOK: null,
    lightConditionsOK: null,
    cleaningOK: null,
    alchoholOK: null,
    moldFungusOK: null,
    relativeHumidityOK: null,
    pestOK: null,
    storageUnit: null,
    temperature: '12',
    temperatureTolerance: '2',
    relativeHumidity: '89',
    relativeHumidityInterval: '4',
    inertAir: '56',
    inertAirInterval: '4',
    light: 'Mørkt',
    cleaning: 'Gullende rent',
    startDate: null, // moment(),
    user: null // this.props.user ? this.props.user.name : ''
  }
}

const controlReducer = (state = initialState, action = {}) => {
  switch (action.type) {
    case ADD: {
      return {
        ...state,
        loading: true,
        loaded: false
      };
    }
    case ADD_SUCCESS:
      return {
        ...state,
        loading: false,
        loaded: true
      };
    case ADD_FAIL:
      return {
        ...state,
        loading: false,
        loaded: false,
        error: action.error
      }
    default:
      return state;
  }
}

export default controlReducer;

export const addControl = (controlData) => {
  const data = mapToBackend(controlData)
  return {
    types: [ADD, ADD_SUCCESS, ADD_FAIL],
    promise: (client) => client.post('/api/event/v1/event', { data })
  }
}
