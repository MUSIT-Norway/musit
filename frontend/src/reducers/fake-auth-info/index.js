const LOGIN = 'musit/fake-auth-info/LOGIN'
const LOGIN_SUCCESS = 'musit/fake-auth-info/LOGIN_SUCCESS'
const LOGIN_FAIL = 'musit/fake-auth-info/LOGIN_FAIL'

const fakeAuthInfo = require('./users.json')
const initialState = {
  ...fakeAuthInfo
}

const fakeAuthInfoReducer = (state = initialState, action = {}) => {
  switch (action.type) {
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

export const login = () => {
  return {
    types: [LOGIN, LOGIN_SUCCESS, LOGIN_FAIL],
    promise: (client) => client.get('/musit')
  }
}
