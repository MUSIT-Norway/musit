export const mapToBackend = (state) => {
  const r = {}
  r.eventType = 'Control'
  r.doneBY = state.doneBy
  r.doneDate = state.doneDate
  r['subEvents-parts'] = Object.keys(state).filter((key) => key.endsWith('OK')).map((key) => {
    switch (key) {
      case 'inertAirOK':
        return {
          eventType: 'ControlInertluft',
          ok: state[key]
        }
      case 'temperatureOK':
        return {
          eventType: 'ControlTemperature',
          ok: state[key]
        }
      case 'gasOK':
        return {
          eventType: 'ControlGass',
          ok: state[key]
        }
      case 'cleaningOK':
        return {
          eventType: 'ControlRenhold',
          ok: state[key]
        }
      case 'relativeHumidityOK':
        return {
          eventType: 'ControlRelativLuftfuktighet',
          ok: state[key]
        }
      case 'lightConditionsOK':
        return {
          eventType: 'ControlLysforhold',
          ok: state[key]
        }
      case 'alcoholOK':
        return {
          eventType: 'ControlSprit',
          ok: state[key]
        }
      case 'pestOK':
        return {
          eventType: 'ControlSkadedyr',
          ok: state[key]
        }
      case 'moldOK':
        return {
          eventType: 'ControlMugg',
          ok: state[key]
        }
      default:
        throw Error(`Unsupported control state key: ${key}`)
    }
  })
  return r
}
