const CLEAR = 'musit/picklist/CLEAR'
const ADD = 'musit/picklist/ADD'
const REMOVE = 'musit/picklist/REMOVE'
const CREATE_LIST = 'musit/picklist/CREATE-LIST'
const REMOVE_LIST = 'musit/picklist/REMOVE-LIST'
const ACTIVATE_LIST = 'musit/picklist/ACTIVATE-LIST'
const TOGGLE_MARKED = 'musit/picklist/TOGGLE_MARKED'

const initialState = {
  active: 'default',
  marked: [],
  lists: {
    default: []
  }
}
const demoState = {
  active: 'default',
  marked: [],
  lists: {
    default: [
      {
        id: 1,
        name: 'Foo'
      },
      {
        id: 2,
        name: 'Bar'
      },
      {
        id: 3,
        name: 'Bas'
      }
    ]
  }
}

const picklistReducer = (state = demoState, action = {}) => {
  const subStateKey = action.destination
  const activeSubStateKey = state.active
  const subState = state.lists[subStateKey] ? state.lists[subStateKey] : []
  let newMarked = []

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
    case TOGGLE_MARKED: {
      console.log(action)
      if (action.id) {
        // We operate on one or several spesific entries
        if (state.marked.indexOf(action.id) >= 0) {
          // it exists, so lets toggle off
          newMarked = state.marked.filter((id) => {
            return id !== action.id
          })
        } else {
          // it does not exist, so lets add
          newMarked = [...state.marked, action.id]
        }
      } else {
        // We operate on all
        if (state.marked.length > 0) {
          // Lets toggle all off
          newMarked = []
        } else {
          // Lets toggle all on
          newMarked = state.lists[activeSubStateKey].map((item) => {
            return item.id
          })
        }
      }
      return {
        ...state,
        marked: newMarked
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

export const toggleMarked = (id) => {
  return {
    type: TOGGLE_MARKED,
    id
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
