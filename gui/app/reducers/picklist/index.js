const CLEAR = 'musit/picklist/CLEAR'
const ADD = 'musit/picklist/ADD'
const REMOVE = 'musit/picklist/REMOVE'
const CREATE_LIST = 'musit/picklist/CREATE-LIST'
const REMOVE_LIST = 'musit/picklist/REMOVE-LIST'
const ACTIVATE_LIST = 'musit/picklist/ACTIVATE-LIST'

const initialState = {
  active: 'default',
  lists: {
    default: []
  }
}
const demoState = {
  active: 'default',
  lists: {
    default: [
      {
        name: 'Foo'
      },
      {
        name: 'Bar'
      },
      {
        name: 'Bas'
      }
    ]
  }
}

const picklistReducer = (state = demoState, action = {}) => {
  const subStateKey = action.destination
  const subState = state.lists[subStateKey] ? state.lists[subStateKey] : []

  switch (action.type) {
    case CLEAR: {
      let retVal = initialState
      if (subStateKey && subStateKey.length > 0) {
        retVal = {
          ...state,
          lists: {
            ...state.lists,
            [subStateKey]: []
          }
        }
      } else {
        retVal = initialState
      }
      return retVal
    }
    case ACTIVATE_LIST:
      return {
        ...state,
        active: action.destination
      }
    case CREATE_LIST:
      return {
        ...state,
        lists: {
          ...state.lists,
          [subStateKey]: []
        }
      }
    case REMOVE_LIST:
      return {
        ...state,
        lists: subState.filter((item) => {
          return item.key !== subStateKey
        })
      }
    case ADD:
      return {
        ...state,
        lists: {
          ...state.lists,
          [subStateKey]: [
            ...subState,
            action.item
          ]
        }
      }
    case REMOVE:
      return {
        ...state,
        lists: {
          ...state.lists,
          [subStateKey]: subState.filter((item) => {
            return item !== action.item
          })
        }
      }
    default:
      return state
  }
}

export default picklistReducer

export const clear = (destination = null) => {
  return {
    type: CLEAR,
    destination
  }
}

export const add = (destination, item) => {
  return {
    type: ADD,
    destination,
    item
  }
}

export const remove = (destination, item) => {
  return {
    type: REMOVE,
    destination,
    item
  }
}

export const activatePickList = (destination) => {
  return {
    type: ACTIVATE_LIST,
    destination
  }
}

export const createPickList = (destination) => {
  return {
    type: CREATE_LIST,
    destination
  }
}

export const removePickList = (destination) => {
  return {
    type: REMOVE_LIST,
    destination
  }
}
