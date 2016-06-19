const real = require('./real')
const fake = require('./fake')
const Button = __FAKE_FEIDE__ && __DEVELOPMENT__ ? fake : real;
export default Button;
