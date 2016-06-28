import { assert } from '../../../../test/setup'
import deepFreeze from 'deep-freeze'

import picklistReducer, {
  clear,
  add,
  remove,
  toggleMarked,
  activatePickList,
  createPickList,
  removePickList,
  initialState
} from '../index'

const testState = {
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
deepFreeze(testState)

describe('PicklistReducer', () => {
  it('Initial state is set', () => {
    const state = picklistReducer(testState, {})
    assert(state === testState)
  })

  it('Clear action, resets state', () => {
    const state = picklistReducer(testState, clear())
    assert(state === initialState)
  })

  it('Add and remove action, adds a record to the picklist and removes it again.', () => {
    const testItem = {
      id: 999,
      name: 'Dynamic'
    }
    const state = picklistReducer(testState, add(testState.active, testItem))
    assert(state.lists.default.length === 4)
    deepFreeze(state)
    const state2 = picklistReducer(state, remove(testState.active, testItem))
    assert(state2.lists.default.length === 3)
  })

  it('Toggle record in list on off', () => {
    const state = picklistReducer(testState, toggleMarked(2))
    assert(state.marked.length === 1)
  })

  it('Add, activate and remove extra picklist', () => {
    const state = picklistReducer(testState, createPickList('test'))
    assert(state.lists.test)
    deepFreeze(state)
    const state2 = picklistReducer(state, activatePickList('test'))
    assert(state2.active === 'test')
    deepFreeze(state2)
    const state3 = picklistReducer(state2, removePickList('test'))
    assert(state3.active === 'default')
    assert(state3.lists.test.length === 0) // TODO: Find an immutable way of removing a property from an object
  })

  it('Fail on activate a list that does not exist', () => {
    const state = picklistReducer(testState, activatePickList('fail'))
    assert(state.active === 'default')
  })
})
