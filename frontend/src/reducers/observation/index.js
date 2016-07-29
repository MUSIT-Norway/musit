const ADD = 'musit/observation/ADD'
const LOAD = 'musit/observation/LOAD'

export const initialState = {
  data: {
    user: '',
    date: '',
    observations: []
  }
}

const observationReducer = (state = initialState, action = {}) => {
  switch (action.type) {
    case ADD:
      return {
        ...state,
        loading: true,
        loaded: false,
        data: action.data
      };
    case LOAD:
      return {
        ...state,
        loading: false,
        loaded: true,
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

export const loadObservation = () => {
  return {
    type: LOAD,
    data: { observations: [
      {
        type: 'pest',
        data: {
          observations: [{ lifeCycle: 'Puppe', count: 4 }, { lifeCycle: 'Egg', count: 4 }],
          identificationValue: 'Test identification value.',
          commentsValue: 'Test comments.'
        }
      },
      {
        type: 'hypoxicAir',
        data: {
          fromValue: '19',
          toValue: '23',
          commentValue: 'Test comments.'
        }
      },
      {
        type: 'cleaning',
        data: {
          leftValue: 'Test cleaning value.',
          rightValue: 'Test comments.'
        }
      },
      {
        type: 'alcohol',
        data: {
          statusValue: 'Tilfredsstillende',
          volumeValue: '12',
          commentValue: 'Sprit comments.'
        }
      }] }
  }
}
