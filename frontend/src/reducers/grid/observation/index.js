

const initialState = {
  data: {
    row_1: {
      museumsNumber: 'C10001',
      uNumber: 1,
      term: 'Gråstein'
    },
    row_2: {
      museumsNumber: 'C10002',
      uNumber: 2,
      term: 'Spydspiss'
    },
    row_3: {
      museumsNumber: 'C10003',
      uNumber: 3,
      term: 'Lavasteinnnn'
    },
    row_4: {
      museumsNumber: 'C10004',
      uNumber: 4,
      term: 'Rar dings'
    }

  }
}


const observationGridReducer = (state = initialState) => {
  return state;
}

export default observationGridReducer
