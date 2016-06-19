const languageData = require('./language.yaml')
const initialState = {
  data: languageData
};

const languageReducer = (state = initialState, action = {}) => {
  switch (action.type) {
    default:
      return state
  }
}

export default languageReducer
