const initialState = {
  data: {
    datePerformed: '2016-05-19T16:00:00.000Z',
    performedBy: 'Test user first',
    dateRegistered: '2013-12-10T16:00:00.000Z',
    registeredBy: 'Test user second',

    control: {
      temperatureControl: {
        ok: true
      },
      relativeHumidity: {
        ok: false
      },
      lightConditionControl: {
        ok: false
      },
      pestControl: {
        ok: false
      },
      alcoholControl: {
        ok: true
      }
    }
  }
};

const authReducer = (state = initialState) => {
  return state;
}

export default authReducer
