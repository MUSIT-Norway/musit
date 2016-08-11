export const mapToBackend = (state) => {
  const r = {}
  r.eventType = 'Control'
  r['subEvents-parts'] = Object.keys(state.data).filter((key) => key.endsWith('OK')).map( (key) => {
    switch (key) {
      case 'inertAirOK':
        return {
          eventType: 'ControlInertluft',
          ok: state.data[key]
        }
      case 'temperatureOK':
        return {
          eventType: 'ControlTemperature',
          ok: state.data[key]
        }
      case 'gasOK':
        return {
          eventType: 'ControlGas',
          ok: state.data[key]
        }
      case 'cleaningOK':
        return {
          eventType: 'ControlCleaning',
          ok: state.data[key]
        }
      case 'relativeHumidityOK':
        return {
          eventType: 'ControlRelativeHumidity',
          ok: state.data[key]
        }
      case 'lightConditionsOK':
        return {
          eventType: 'ControlLightConditions',
          ok: state.data[key]
        }
      case 'alcoholOK':
        return {
          eventType: 'ControlAlcohol',
          ok: state.data[key]
        }
      case 'pestOK':
        return {
          eventType: 'ControlPest',
          ok: state.data[key]
        }
      case 'moldFungusOK':
        return {
          eventType: 'ControlMoldFungus',
          ok: state.data[key]
        }
      default:
        throw Error("Unsupported control state key: " + key)
    }
  })
  return r
}