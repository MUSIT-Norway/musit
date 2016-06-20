const CREATE = 'musit/organization/CREATE'
const CREATE_SUCCESS = 'musit/organization/CREATE_SUCCESS'
const CREATE_FAIL = 'musit/organization/CREATE_FAIL'

const initialState = {
  loading: false,
  loaded: false
}

const organizationReducer = (state = initialState, action = {}) => {
  switch (action.type) {
    case CREATE:
      return {
        ...state,
        loading: true
      }
    case CREATE_SUCCESS:
      return {
        loading: false,
        loaded: true,
        data: action.result
      }
    case CREATE_FAIL:
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

export default organizationReducer

export const createOrganization = (organization) => {
  return {
    types: [CREATE, CREATE_SUCCESS, CREATE_FAIL],
    promise: (client) => client.post('/api/actor/v1/organization', { data: organization })
  }
}
