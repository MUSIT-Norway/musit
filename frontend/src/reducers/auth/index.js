const SET_USER = 'musit/auth/SET_USER';
const CLEAR_USER = 'musit/auth/CLEAR_USER';

const loadFromStorage = () => {
  if (localStorage.getItem('musitUserName')) {
    return {
      name: localStorage.getItem('musitUserName'),
      accessToken: localStorage.getItem('musitAccessToken'),
      email: localStorage.getItem('musitUserEmail'),
      userId: localStorage.getItem('musitUserId')
    }
  }
  return null
}

const initialState = {
  user: loadFromStorage()
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
  localStorage.removeItem('musitAccessToken');
  localStorage.removeItem('musitUserId');
  localStorage.removeItem('musitUserName');
  localStorage.removeItem('musitUserAvatar');
  localStorage.removeItem('musitUserEmail');
  return {
    type: CLEAR_USER
  };
}
