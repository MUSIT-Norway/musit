import { mapToFrontEnd, mapToBackEnd } from './mapper'
const ADD = 'musit/observation/ADD'
const ADD_SUCCESS = 'musit/observation/ADD_SUCCESS'
const ADD_FAIL = 'musit/observation/ADD_FAIL'
const LOAD = 'musit/observation/LOAD'
const LOAD_SUCCESS = 'musit/observation/LOAD_SUCCESS'
const LOAD_FAIL = 'musit/observation/LOAD_FAIL'

export const initialState = {
  data: {
    user: '',
    date: '',
    observations: []
  }
}

const observationReducer = (state = initialState, action = {}) => {
  let d = {}
  switch (action.type) {
    case ADD:
      return {
        ...state,
        loading: true,
        loaded: false,
        data: {}
      };
    case ADD_SUCCESS:
      d = mapToFrontEnd(action.result)
      return {
        ...state,
        loading: false,
        loaded: true,
        data: d
      };
    case ADD_FAIL:
      return {
        ...state,
        loading: false,
        loaded: false,
        data: action.error
      }
    case LOAD:
      return {
        ...state,
        loading: true,
        loaded: false,
        data: {}
      };
    case LOAD_SUCCESS:
      d = mapToFrontEnd(action.result)
      return {
        ...state,
        loading: false,
        loaded: true,
        data: d
      };
    case LOAD_FAIL:
      return {
        ...state,
        loading: false,
        loaded: false,
        data: action.error
      };
    default:
      return state;
  }
}

export default observationReducer;

export const addObservation = (data) => {
  const action = 'post'
  const url = '/api/event/v1/event'
  const dataToPost = mapToBackEnd(data)
  return {
    types: [ADD, ADD_SUCCESS, ADD_FAIL],
    promise: (client) => client[action](url, { data: dataToPost })
  };
}


export const loadObservation = (id) => {
  // const fromService = {
  //   eventType: 'Observation',
  //   note: 'tekst til observasjonene',
  //   links: [{ rel: 'actor', href: 'actor/12' }],
  //   subEvents: [
  //   { eventType: 'observationTemperature', temperatureFrom: '25', temperatureTo: '30',
  //   note: 'tekst til observationTemperature' },
  //   { eventType: 'observationRelativeHumidity', humidityFrom: '40', humidityTo: '50',
  //     note: 'tekst til observationRelativeHumidity' },
  //   { eventType: 'observationInertLuft', inertLuftFrom: '40', inertLuftTo: '50', note: 'tekst til observationInertLuft' },
  //   { eventType: 'observationLight', lysforhold: 'for mørkt', note: 'tekst til observationLight' },
  //   { eventType: 'observationRenhold', renhold: 'støv i krokene', note: 'tekst til observationRenhold' },
  //   { eventType: 'observationGass', gass: 'alt for mye gass i rommet', note: 'tekst til observationGass' },
  //   { eventType: 'observationMugg', mugg: 'alt for mye mugg her', note: 'tekst til observationMugg' },
  //   { eventType: 'observationTyveriSikring', tyveriSikring: 'alt for mye tyverisikring her',
  //     note: 'tekst til observationTyveriSikring' },
  //   { eventType: 'observationBrannSikring', brannSikring: 'alt for lite brannsikring',
  //     note: 'tekst til observationBrannsikring' },
  //   { eventType: 'observationSkallSikring', skallSikring: 'alt for mye skallsikring her',
  //     note: 'tekst til observationSkallSikring' },
  //   { eventType: 'observationVannskadeRisiko', vannskadeRisiko: 'vannskade i hjørnet',
  //     note: 'tekst til observationVannskadeRisiko' },
  //   { eventType: 'observationSkadedyr',
  //   identifikasjon: 'skadedyr i veggene',
  //   note: 'tekst til observationskadedyr',
  //   livssykluser: [
  //   { livssyklus: 'Adult', antall: '3' },
  //   { livssyklus: 'Puppe', antall: '4' },
  //   { livssyklus: 'Puppeskin', antall: '5' },
  //   { livssyklus: 'Larva', antall: '6' },
  //   { livssyklus: 'Egg', antall: '7' }]
  //   },
  //   { eventType: 'observationSprit',
  //     note: 'tekst til observationsprit',
  //   Tilstander: [
  //   { Tilstand: 'Uttørket', Volum: '3,2' },
  //   { Tilstand: 'nesten uttørket', Volum: '4,554' },
  //   { Tilstand: 'noe uttørket', Volum: '5,332' },
  //   { Tilstand: 'litt uttørket', Volum: '6,3' },
  //   { Tilstand: 'tilfredsstillende', Volum: '7' }]
  //   }]
  // }
  // const toService = mapToFrontEnd(fromService)
  // return {
  //   type: LOAD,
  //   data: toService
  //
  // }
  return {
    types: [LOAD, LOAD_SUCCESS, LOAD_FAIL],
    promise: (client) => client.get(`api/event/v1/event/${id}`)
  }
}
