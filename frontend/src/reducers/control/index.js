const ADD = 'musit/control/ADD'

const initialState = {
  user: '',
  date: '',
  data: {
    temperatureOK: null,
    inertAirOK: null,
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
    case ADD: return {
      ...state,
      loading: true,
      loaded: false,
      data: action.data
    };
    default:
      return state;
  }
}

export default controlReducer;

export const addControl = (data) => {
  return {
    type: ADD,
    data
  }
}
