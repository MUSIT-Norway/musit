import { mapToFrontEnd } from './mapper'
const ADD = 'musit/observation/ADD'
const LOAD = 'musit/observation/LOAD'

export const initialState = {
  data: {
    user: '',
    date: '',
    observations: []
  }
}

const observationReducer = (state = initialState, action = {}) => {
  switch (action.type) {
    case ADD:
      return {
        ...state,
        loading: true,
        loaded: false,
        data: action.data
      };
    case LOAD:
      return {
        ...state,
        loading: false,
        loaded: true,
        data: action.data
      };
    default:
      return state;
  }
}

export default observationReducer;

export const addObservation = (data) => {
  return {
    type: ADD,
    data
  }
}

export const loadObservation = () => {
  const fromService = {
    eventType: 'Observation',
    note: 'tekst til observasjonene',
    links: [{ rel: 'actor', href: 'actor/12' }],
    subevents: [
    { eventType: 'observationTemperature', temperature_from: 25, temperature_to: 30,
    note: 'tekst til observationTemperature' },
    { eventType: 'observationRelativeHumidity', humidity_from: 40, humidity_to: 50,
      note: 'tekst til observationRelativeHumidity' },
    { eventType: 'observationInertLuft', inertLuft_from: 40, inertLuft_to: 50, note: 'tekst til observationInertLuft' },
    { eventType: 'observationLight', Lysforhold: 'for mørkt', note: 'tekst til observationLight' },
    { eventType: 'observationRenhold', Renhold: 'støv i krokene', note: 'tekst til observationRenhold' },
    { eventType: 'observationGass', Gass: 'alt for mye gass i rommet', note: 'tekst til observationGass' },
    { eventType: 'observationMugg', Mugg: 'alt for mye mugg her', note: 'tekst til observationMugg' },
    { eventType: 'observationTyveriSikring', TyveriSikring: 'alt for mye tyverisikring her',
      note: 'tekst til observationTyveriSikring' },
    { eventType: 'observationBrannSikring', BrannSikring: 'alt for lite brannsikring',
      note: 'tekst til observationBrannsikring' },
    { eventType: 'observationSkallSikring', SkallSikring: 'alt for mye skallsikring her',
      note: 'tekst til observationSkallSikring' },
    { eventType: 'observationVannskadeRisiko', VannskadeRisiko: 'vannskade i hjørnet',
      note: 'tekst til observationVannskadeRisiko' },
    { eventType: 'observationSkadedyr',
    identifikasjon: 'skadedyr i veggene',
    note: 'tekst til observationskadedyr',
    livssykluser: [
    { livssyklus: 'Adult', antall: 3 },
    { livssyklus: 'Puppe', antall: 4 },
    { livssyklus: 'Puppeskinn', antall: 5 },
    { livssyklus: 'Larve', antall: 6 },
    { livssyklus: 'Egg', antall: 7 }]
    },
    { eventType: 'observationSprit',
      note: 'tekst til observationsprit',
    Tilstander: [
    { Tilstand: 'Uttørket', Volum: '3,2' },
    { Tilstand: 'nesten uttørket', Volum: '4,554' },
    { Tilstand: 'noe uttørket', Volum: '5,332' },
    { Tilstand: 'litt uttørket', Volum: '6,3' },
    { Tilstand: 'tilfredsstillende', Volum: 7 }]
    }]
  }
  const toService = mapToFrontEnd(fromService)
  return {
    type: LOAD,
    data: toService
    { observations: [
      {
        type: 'pest',
        data: {
          observations: [{ lifeCycle: 'Puppe', count: 4 }, { lifeCycle: 'Egg', count: 4 }],
          identificationValue: 'Test identification value.',
          commentsValue: 'Test comments.'
        }
      },
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
      },
      {
        type: 'alcohol',
        data: {
          statusValue: 'Tilfredsstillende',
          volumeValue: '12',
          commentValue: 'Sprit comments.'
        }
      }] }
  }
}
