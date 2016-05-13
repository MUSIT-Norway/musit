const LOAD = 'musit/fake-auth-info/LOAD'
const LOAD_SUCCESS = 'musit/fake-auth-info/LOAD_SUCCESS'
const LOAD_FAIL = 'musit/fake-auth-info/LOAD_FAIL'
const LOGIN = 'musit/fake-auth-info/LOGIN'
const LOGIN_SUCCESS = 'musit/fake-auth-info/LOGIN_SUCCESS'
const LOGIN_FAIL = 'musit/fake-auth-info/LOGIN_FAIL'

const initialState = {
  loaded: false
}

const fakeAuthInfoReducer = (state = initialState, action = {}) => {
  switch (action.type) {
    case LOAD:
      return {
        ...state,
        loading: true
      }
    case LOAD_SUCCESS:
      return {
        ...state,
        loading: false,
        loaded: true,
        users: action.result
      }
    case LOAD_FAIL:
      return {
        ...state,
        loading: false,
        loaded: false,
        error: action.error
      }
    case LOGIN:
      return {
        ...state,
        loading: true
      }
    case LOGIN_SUCCESS:
      return {
        ...state,
        loading: false,
        loaded: true,
        user: action.result.user
      }
    case LOGIN_FAIL:
      return {
        ...state,
        loading: false,
        loaded: false,
        error: action.error
      }
    default:
      return state
  }
}

export default fakeAuthInfoReducer

export const isLoaded = (globalState) => {
  return globalState.fakeAuthInfo && globalState.fakeAuthInfo.loaded
}

export const load = () => {
  return {
    types: [LOAD, LOAD_SUCCESS, LOAD_FAIL],
    promise: (client) => client.get('/loadFakeAuthInfo')
  }
}

export const login = (username) => {
  return {
    types: [LOGIN, LOGIN_SUCCESS, LOGIN_FAIL],
    promise: (client) => client.post('/login', {
      data: {
        username: username,
        password: ''
      }
    })
  }
}
