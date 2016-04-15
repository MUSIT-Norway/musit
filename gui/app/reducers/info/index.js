const LOAD = 'musit/LOAD';
const LOAD_SUCCESS = 'musit/LOAD_SUCCESS';
const LOAD_FAIL = 'musit/LOAD_FAIL';

const initialState = {
  loaded: false
};

const infoReducer = (state = initialState, action = {}) => {
  switch (action.type) {
    case LOAD:
      return {
        ...state,
        loading: true
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
      };
    default:
      return state;
  }
}

export default infoReducer

export const isLoaded = (globalState) => {
  return globalState.info && globalState.info.loaded;
}

export const load = () => {
  return {
    types: [LOAD, LOAD_SUCCESS, LOAD_FAIL],
    promise: (client) => client.get('/loadInfo')
  }
}
