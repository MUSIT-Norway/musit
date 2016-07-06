const initialState = {
  data: {
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
      ok: false
    }
  }
};

const authReducer = (state = initialState) => {
  return state;
}

export default authReducer
