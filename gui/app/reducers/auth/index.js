const SET_USER = 'musit/auth/SET_USER';
const CLEAR_USER = 'musit/auth/CLEAR_USER';

const initialState = {
  user: null
};

const authReducer = (state = initialState, action = {}) => {
  switch (action.type) {
    case SET_USER:
      return {
        ...state,
        user: action.user
      };
    case CLEAR_USER:
      return {
        ...state,
        user: null
      };
    default:
      return state;
  }
}

export default authReducer

export const connectUser = (user) => {
  return {
    type: SET_USER,
    user: user
  }
}

export const clearUser = () => {
  return {
    type: CLEAR_USER
  };
}
