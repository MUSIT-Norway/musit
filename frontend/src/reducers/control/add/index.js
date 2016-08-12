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
      const data = mapToBackend(action.data)
      return {
        ...state,
        loading: true,
        loaded: false,
        data
      };
    }
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
