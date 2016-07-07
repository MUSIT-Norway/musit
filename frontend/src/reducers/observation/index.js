const ADD = 'musit/observation/ADD'

const initialState = {
  user: '',
  date: '',
  data: {
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
