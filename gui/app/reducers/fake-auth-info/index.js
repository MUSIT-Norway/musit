const LOAD = 'musit/fake-auth-info/LOAD';
const LOAD_SUCCESS = 'musit/fake-auth-info/LOAD_SUCCESS';
const LOAD_FAIL = 'musit/fake-auth-info/LOAD_FAIL';

const initialState = {
  loaded: false
};

const fakeAuthInfoReducer = (state = initialState, action = {}) => {
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
        users: action.result
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

export default fakeAuthInfoReducer

export const isLoaded = (globalState) => {
  return globalState.fakeAuthInfo && globalState.fakeAuthInfo.loaded;
}

export const load = () => {
  return {
    types: [LOAD, LOAD_SUCCESS, LOAD_FAIL],
    promise: (client) => client.get('/loadFakeAuthInfo')
  };
}
