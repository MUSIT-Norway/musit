const ADD = 'musit/observation/ADD'

const initialState = {
  data: {
    user: '',
    date: '',
    observations: []
  }
}


const observationReducer = (state = initialState, action = {}) => {
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

export default observationReducer;

export const addObservation = (data) => {
  return {
    type: ADD,
    data
  }
}
