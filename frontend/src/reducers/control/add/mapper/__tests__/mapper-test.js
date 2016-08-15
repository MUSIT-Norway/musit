import { assert } from '../../../../../../test/setup'
import deepFreeze from 'deep-freeze'

import {
  mapToBackend
} from '../to_backend'

describe('ControlMapperReducer', () => {
  it('maps to correct backend structure', () => {
    const state = {
      user: 'jarl',
      date: 'some time',
      temperatureOK: true,
      inertAirOK: false,
      gasOK: true,
      cleaningOK: true,
      relativeHumidityOK: true,
      lightConditionsOK: true,
      alcoholOK: true,
      pestOK: false,
      moldOK: true
    }
    deepFreeze(state)
    const transformed = mapToBackend(state)
    assert(transformed['subEvents-parts'][0].eventType === 'ControlTemperature')
    assert(transformed['subEvents-parts'][0].ok === true)
    assert(transformed['subEvents-parts'][1].eventType === 'ControlInertluft')
    assert(transformed['subEvents-parts'][1].ok === false)
    assert(transformed['subEvents-parts'][2].eventType === 'ControlGass')
    assert(transformed['subEvents-parts'][2].ok === true)
    assert(transformed['subEvents-parts'][3].eventType === 'ControlRenhold')
    assert(transformed['subEvents-parts'][3].ok === true)
    assert(transformed['subEvents-parts'][4].eventType === 'ControlRelativLuftfuktighet')
    assert(transformed['subEvents-parts'][4].ok === true)
    assert(transformed['subEvents-parts'][5].eventType === 'ControlLysforhold')
    assert(transformed['subEvents-parts'][5].ok === true)
    assert(transformed['subEvents-parts'][6].eventType === 'ControlSprit')
    assert(transformed['subEvents-parts'][6].ok === true)
    assert(transformed['subEvents-parts'][7].eventType === 'ControlSkadedyr')
    assert(transformed['subEvents-parts'][7].ok === false)
    assert(transformed['subEvents-parts'][8].eventType === 'ControlMugg')
    assert(transformed['subEvents-parts'][8].ok === true)
  })
})
