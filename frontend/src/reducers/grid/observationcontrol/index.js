

const initialState = {
  data: [
    {
      id: 1,
      type: 'control',
      date: '01.01.1983',
      types: { temperature: true,
        inertAir: null,
        relativeHumidity: null,
        cleaning: null,
        lightCondition: null,
        alchohol: true,
        gas: null,
        mold: null,
        pest: true,
        envdata: null },
      doneBy: 'Blablabla...',
      registeredDate: '01.01.1983',
      registeredBy: 'Blabla...'
    },
    {
      id: 2,
      type: 'observation',
      date: '01.01.1984',
      types: { temperature: true,
        inertAir: null,
        relativeHumidity: null,
        cleaning: null,
        lightCondition: true,
        alchohol: false,
        gas: null,
        mold: null,
        envdata: null },
      doneBy: 'Blablabla...',
      registeredDate: '01.01.1983',
      registeredBy: 'Blabla...'
    },
    {
      id: 3,
      type: 'observation',
      date: '01.01.1984',
      types: { temperature: true,
        inertAir: true,
        relativeHumidity: true,
        cleaning: true,
        lightCondition: true,
        alchohol: true,
        gas: true,
        mold: true,
        envdata: true },
      doneBy: 'Blablabla...',
      registeredDate: '01.01.1983',
      registeredBy: 'Blabla...'
    },
    {
      id: 4,
      type: 'control',
      date: '01.01.1984',
      types: { temperature: false,
        inertAir: false,
        relativeHumidity: false,
        cleaning: false,
        lightCondition: false,
        alchohol: false,
        gas: false,
        mold: false,
        envdata: false },
      doneBy: 'Blablabla...',
      registeredDate: '01.01.1983',
      registeredBy: 'Blabla...'
    },
    {
      id: 5,
      type: 'observation',
      date: '01.01.1984',
      types: { temperature: null,
        inertAir: null,
        relativeHumidity: null,
        cleaning: null,
        lightCondition: null,
        alchohol: null,
        gas: null,
        mold: null,
        envdata: null },
      doneBy: 'Blablabla...',
      registeredDate: '01.01.1983',
      registeredBy: 'Blabla...'
    },
    {
      id: 6,
      type: 'control',
      date: '01.01.1984',
      types: { temperature: false
         },
      doneBy: 'Blablabla...',
      registeredDate: '01.01.1983',
      registeredBy: 'Blabla...'
    }
  ]
}


const observationControlGridReducer = (state = initialState) => {
  return state;
}

export default observationControlGridReducer
