const validateString = (value, props) => {
  if (!props) {
    props = {
      minimumLength: 1,
      maximumLength: 20
    }
  }
  const isSomething = value.length >= props.minimumLength
  const isValid = isSomething ? 'success' : null
  return value.length > props.maximumLength ? 'error' : isValid
}

const validateNumber = (value, props) => {
  if (!props) {
    props = {
      minimumLength: 1
    }
  }
  const isSomething = value.length >= props.minimumLength
  const isValid = isSomething ? 'success' : null
  return isSomething && isNaN(value) ? 'error' : isValid
}

const validate = (source, value, validateType = 'text', props) => {
  let lValue = ''
  if (source.props.validator) {
    lValue = source.props.validator(value, props)
  } else {
    switch (validateType) {
      case 'text':
        lValue = validateString(value, props)
        break
      case 'number':
        lValue = validateNumber(value, props)
        break
      default:
        lValue = null
    }
  }
  return lValue
}

export { validate }
