import { assert } from '../../../../test/setup'
import deepFreeze from 'deep-freeze'
import observationReducer, {
  addObservation,
  loadObservation,
  initialState
} from '../index'

describe('ObservationReducer', () => {
  it('Initial state is set', () => {
    const state = observationReducer()
    assert(state === initialState)
  })

  it('load observation should update state', () => {
    const state = observationReducer(initialState, loadObservation(1))
    assert(state.data.observations.length === 4)
  })

  it('add observation should update state', () => {
    const fromServer = deepFreeze({
      observations: [
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
        }
      ]
    })
    const state = observationReducer(initialState, addObservation(fromServer))
    assert(state.data === fromServer)
  })
})
