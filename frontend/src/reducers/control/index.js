const ADD = 'musit/control/ADD'
const ADD_SUCCESS = 'musit/control/ADD_SUCCESS'
const ADD_FAILURE = 'musit/control/ADD_FAILURE'

const initialState = {
  user: '',
  date: '',
  data: {
    temperatureOK: '',
    inertAirOK: '',
    cleaningOK: '',
    relativeHumidity: '',
    lightConditionsOK: '',
    alchoholOK: '',
    pestOK: '',
    moldFungusOK: ''
  }
}

const controlReducer = (state = initialState, action = {}) => {
  switch (action.type) {
    case ADD: return {
      ...state,
      data: action.data,
      loading: true,
      loaded: false
    }
    case ADD_SUCCESS: return {
      ...state,
      data: action.data,
      loading: false,
      loaded: true
    }
    case ADD_FAILURE: return {
      ...state,
      loading: false,
      loaded: false,
      error: action.error
    }
    default:
      return state
  }
}

export default controlReducer
